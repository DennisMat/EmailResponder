package com.dennis.emailresponder;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Date;
import java.util.Properties;
import javax.mail.*;
import javax.mail.Message.RecipientType;
import javax.mail.internet.*;
import javax.mail.search.FlagTerm;

class Methods {



	static void processUnreadEmails(String email, String password, String host) {

		int minutes = 10;
		try {
			// while(true) {
			scanUnreadEmails(email, password, host);
			// Thread.sleep(1000*60*minutes);
			// }
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	static void scanUnreadEmails(String email, String password, String host) {
		System.out.println("\n\nNow scanning emails...\n");
		// Properties props = System.getProperties();
		Properties props = setAndGetProperties();

		Session session = Session.getDefaultInstance(props, null);

		try {
			Store store = session.getStore("imaps");
			store.connect(host, email, password);

			Folder inbox = store.getFolder("INBOX");
			Folder trash = store.getFolder("[Gmail]/Trash");

			inbox.open(Folder.READ_WRITE);

			Flags seen = new Flags(Flags.Flag.SEEN);
			FlagTerm unseenFlagTerm = new FlagTerm(seen, false);
			Message messages[] = inbox.search(unseenFlagTerm);

			int count = 0;

			for (Message msg : messages) {
				System.out.println("\n\n------------Message # " + ++count + "----------------");
				String subject = msg.getSubject().toLowerCase();

				String from = ((InternetAddress) msg.getFrom()[0]).getAddress();
				String toList = parseAddresses(msg.getRecipients(RecipientType.TO));
				String ccList = parseAddresses(msg.getRecipients(RecipientType.CC));

				Date date = msg.getReceivedDate();

				boolean skippable = isSkippable(msg, subject, from, toList, ccList);

				if (skippable) {
					continue;
				}

				boolean isDelete = isDeletetable(msg, subject, from, toList, ccList);

				if (isDelete) {
					softDelete(inbox, trash, msg);
					continue;
				}

				if (!subject.contains("java")) {
					System.out.println("Skipping this email subject=" + msg.getSubject());
					continue;
				}

				String content = getContent(msg);

				String contentLowerCase = content.toLowerCase();
				
				if (contentLowerCase.contains("java")) {

					System.out.println("will respond to Subject = " + msg.getSubject());
					// System.out.println("Content = " + content);

					String responseMessage1 = "Is the C2C rate above $110? (lesser for remote) <br /> "
							+ "What is the salary/rate? <br />"
							+ "Is it available for H1B holders? (I'm Canadian citizen and have a US H1B)<br />"
							+ "Dennis";

					String responseMessage2 = "What is the C2C rate if it's a contract?<br /> "
							+ "What is the salary/rate if it's a FTE? <br />"
							+ "Is it available for H1B holders? (I'm Canadian citizen and have a US H1B)<br />"
							+ "Dennis";

					String responseMessage = "What is the C2C rate if it's a contract?<br /> "
							+ "is it above $110/hr and remote? <br />"
							+ "Is it available for H1B holders? (I'm Canadian citizen and have a US H1B)<br />"
							+ "Dennis";

					responseMessage = modifyContent(content, responseMessage, from, date);

					boolean isSuccess = send(email, password, from, "Re: " + msg.getSubject(), responseMessage,
							session);

					if (isSuccess) {
						System.out.println("Deleting this email after responding. subject = " + msg.getSubject());
						softDelete(inbox, trash, msg);
					}

					// inbox.close(false);
				} else {
					System.out.println("Skipping 5 this email subject=" + msg.getSubject());
					continue;
				}

			}
		} catch (Exception e) {
			System.out.println(e);
		}

		System.out.println("\nAll emails processed\n");
	}

	private static boolean isSkippable(Message msg, String subject, String from, String toList, String ccList) {

		boolean skippable = false;
		try {
			if (from.endsWith("@stellarit.com") || toList.contains("@stellarit.com")
					|| ccList.contains("@stellarit.com")) {
				skippable = true;
				System.out.println("Skipping this email because it's form stellarit.com. Subject=" + msg.getSubject());

			} else if (subject.startsWith("re:")) {
				System.out.println("Skipping this email because it's a reply. Subject=" + msg.getSubject());
				skippable = true;

			} else if (subject.startsWith("rtr")) {
				System.out.println("Skipping this email because this is an RTR. Subject=" + msg.getSubject());
				skippable = true;
			}

			String content = getContent(msg);

			String contentLowerCase = content.toLowerCase();

			// these are invites.
			if (contentLowerCase.contains("webex") || contentLowerCase.contains("meet.google.com")
					|| contentLowerCase.contains("teams.microsoft")|| contentLowerCase.contains("zoom")) {
				System.out.println("Skipping this email because it's an invite subject=" + msg.getSubject());
				skippable = true;
			}

		} catch (Exception e) {
			System.out.println(e);
		}

		return skippable;
	}

	private static boolean isDeletetable(Message msg, String subject, String from, String toList, String ccList) {

		boolean isDeletetable = false;

		try {

			// move this to a properties file
			String[] deleteKeywords = {"citizen only", "performance engineer", "performance test engineer", "c developer", "endeca",
					"BlueYonder", "angular", ".net", "mulesoft", "mule soft", "automation", "big data",
					"business analyst", "etl developer", "new voicemail", "qa tech", "qa lead", "hadoop", "node.js",
					"nodejs", "ui engineer", "android", "data analyst", "etl", "data engineer", "embedded",
					"systems analyst", "system analyst", "quality analyst", "dotnet", "osb developer", "test analyst",
					"ux designer", "ui architect", "tester", "splunk", "new text message", "ui lead", "ui developer",
					"informatica", "front end", "azure", "ebs order management", "ios developer", "weblogic",
					"machine learning", "sap ", "pl/sql developer", "quality assurance", "oracle financials",
					"oracle app", "peoplesoft", "aws dev", "product manager", "operation manager", "splunk", "tableau",
					"c++", "data architect", "support", "ruby", "dynamics", "hl7", "qa analyst", "sdet", "mainframe",
					"tableau", ".net developer", "oracle soa", "c programmer", "android lead", "wireless", "python",
					"salesforce", "react", "qa analyst", "qa engineer", "full stack engineer", "cloud architect",
					"devops", "zuora", "guidewire", "test lead", "apache camel", "oracle dba", "bigdata", "osb",
					"manufacturing", "network", "cyberark", "flexera", "security engineer", "4gl", "sterling",
					"incorta", "ace developer", "oracle atg", "mobile test", "apache spark", "tibco", "modeler", "ormb",
					"wi-fi", "scrum master", "map reduce", "ml engineer", "websphere", "Oracle epm", "pega"

			};

			boolean keyWordFound = false;
			String keyWord = "";
			for (String d : deleteKeywords) {
				if (subject.contains(d)) {
					System.out.println("delete keyword =" + d);
					keyWordFound = true;
					keyWord = d;
					break;
				}
			}

			if (keyWordFound) {
				System.out.println("Deleting  this email keyword: " + keyWord + "from: " + from + " ccList: " + ccList
						+ "subject: " + msg.getSubject());

				isDeletetable = true;
			}

			String[] deleteEmailsFrom = { "jobalerts-noreply@linkedin.com", "invitations@linkedin.com" };
			boolean emailFound = false;
			if (!keyWordFound) {

				String email = "";
				for (String e : deleteEmailsFrom) {
					if (from.contains(e)) {
						System.out.println("from email =" + e);
						emailFound = true;
						email = e;
						break;
					}
				}
			}

			if (emailFound) {
				System.out.println("Deleting  this email because it is from: " + from + " ccList: " + ccList
						+ "subject: " + msg.getSubject());

				isDeletetable = true;
			}

			/*
			 * if((subject.contains("full time") || subject.contains("permanent") ||
			 * subject.contains("fulltime") || subject.contains("fte") )
			 * 
			 * && !(subject.contains("contract") || subject.contains("c2c") ||
			 * subject.contains("cwr") )) {
			 * System.out.println("Deleting 3 this email from: " + from + " ccList: " +
			 * ccList + "subject: " + msg.getSubject()); softDelete(inbox, trash, msg);
			 * continue; }
			 */
			if (subject.contains("w2 only")) {
				isDeletetable = true;
				System.out.println("Deleting this email because it a w2 only. subject=" + msg.getSubject());

			}

		} catch (Exception e) {
			System.out.println(e);
		}

		return isDeletetable;
	}

	static void softDelete(Folder inbox, Folder trash, Message msg) throws MessagingException {
		Message[] msgArr = new Message[1];
		msgArr[0] = msg;
		inbox.copyMessages(msgArr, trash);
		// This line is not needed the above line will handle it
		// msg.setFlag(FLAGS.Flag.DELETED, true);
	}

	static String getContent(Message msg) throws IOException, MessagingException, Exception {

		String content = null;
		String contentType = msg.getContentType().toLowerCase();
		if (contentType.contains("multipart")) {
			// content may contain attachments
			// javax.mail.internet.MimeMultipart multiPart =
			// (javax.mail.internet.MimeMultipart) message.getContent();
			Multipart multiPart = (Multipart) msg.getContent();

			int numberOfParts = multiPart.getCount();
			for (int partCount = 0; partCount < numberOfParts; partCount++) {
				MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(partCount);
				if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {

				} else {
					// this part may be the message content
					// content = part.getContent().toString();
					content = dumpPart(part);
					int a = 3;
				}
			}

		} else if (contentType.contains("text/plain") || contentType.contains("text/html")) {
			Object ob = msg.getContent();
			if (ob != null) {
				content = ob.toString();
			}
		}
		return content;
	}

	static Properties setAndGetProperties() {
		// Get properties object
		Properties props = new Properties();
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.socketFactory.port", "465");
		props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", "465");
		props.setProperty("mail.store.protocol", "imaps");

		/*
		 * This also works keep just in case. Notice the different post.
		 * props.put("mail.smtp.host", "smtp.gmail.com");
		 * props.put("mail.smtp.socketFactory.port", "587");
		 * props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		 * props.put("mail.smtp.auth", "true"); props.put("mail.smtp.port", "587");
		 * props.put("mail.smtp.starttls.enable", "true");
		 * props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
		 * props.put("mail.smtp.socketFactory.fallback", "true");
		 * props.put("mail.smtp.ssl.socketFactory", "true");
		 * props.put("mail.smtp.EnableSSL.enable","true");
		 * props.put("mail.smtp.ssl.protocols","TLSv1.2");
		 */
		return props;
	}

	/*
	 * Not sure if this is working, but this method may never be never touched.
	 */
	static String dumpPart(Part p) throws Exception {
		String contentType = p.getContentType();
		// System.out.println("dumpPart" + contentType);
		InputStream is = p.getInputStream();
		if (!(is instanceof BufferedInputStream)) {
			is = new BufferedInputStream(is);
		}
		int c;
		final StringWriter sw = new StringWriter();
		while ((c = is.read()) != -1) {
			sw.write(c);
		}

		return sw.toString();
	}

	static String modifyContent(String content, String responseMessage, String from, Date date) throws Exception {

		return responseMessage + "<br /><br />" + date.toString() + " &lt;" + from + "&gt; wrote: <br />" + content;
	}

	static boolean send(final String email, final String password, String to, String sub, String msg, Session session) {
		boolean isSuccess = false;
		Authenticator auth = new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(email, password);
			}
		};

		Properties props = setAndGetProperties();
		session = Session.getInstance(props, auth);

		// compose message
		try {
			MimeMessage message = new MimeMessage(session);
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
			message.setSubject(sub);
			message.setText(msg, "utf-8", "html");
			// send message
			Transport.send(message);
			System.out.println("Message sent successfully to: " + to);
			isSuccess = true;
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}

		return isSuccess;
	}

	/**
	 * Returns a list of addresses in String format separated by comma
	 *
	 * @param address an array of Address objects
	 * @return a string represents a list of addresses
	 */
	static String parseAddresses(Address[] address) {
		String listAddress = "";

		if (address != null) {
			for (int i = 0; i < address.length; i++) {
				listAddress += address[i].toString() + ", ";
			}
		}
		if (listAddress.length() > 1) {
			listAddress = listAddress.substring(0, listAddress.length() - 2);
		}

		return listAddress;
	}

}
