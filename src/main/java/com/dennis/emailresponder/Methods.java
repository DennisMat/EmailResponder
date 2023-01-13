package com.dennis.emailresponder;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.*;
import javax.mail.Message.RecipientType;
import javax.mail.internet.*;
import javax.mail.search.FlagTerm;

class Methods {


	static int minutesSkip = 2;// skip emails that came in the last 10 minutes;

	static long daysInclude = 90;// 3 months
	static String filePathEmailSent = "C:\\temp\\SentEmails.txt";

	public static void main(String[] args)

	{

		fixTLSIssue();

		Methods.processUnreadEmails(email, password, host);

	}

	/**
	 * Newer version of java disables TLSv1 and TLSv1.1, we enable it again.
	 *
	 */
	static void fixTLSIssue() {
		String disabledAlgorithmsProp = "jdk.tls.disabledAlgorithms";
		String newpropValue = java.security.Security.getProperty(disabledAlgorithmsProp).replace("TLSv1, TLSv1.1,", "");
		java.security.Security.setProperty(disabledAlgorithmsProp, newpropValue);
	}

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

			Set<String> sentEmailAddresses = getSentEmailAddresses(store);

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

				Date msgDate = msg.getReceivedDate();

				Date currentTime = new Date();

				if (currentTime.getTime() - msgDate.getTime() < minutesSkip * 60 * 1000) {
					System.out.println("Skipping this email because its recent. subject=" + msg.getSubject());
					continue;
				}

				boolean skippable = isSkippable(msg, subject, from, toList, ccList, sentEmailAddresses);

				if (skippable) {
					continue;
				}

				boolean isDelete = isDeleteable(msg, subject, from, toList, ccList);

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

					String responseMessage1 = "Is the C2C rate above $110?( lesser for remote)<br /> "
							+ "What is the base salary if it's FTE? <br />" + "Dennis";

					String responseMessage2 = "What is the C2C rate if it's a contract?<br /> "
							+ "What is the salary/rate if it's a FTE? <br />"
							+ "Is it available for H1B holders? (I'm Canadian citizen and have a US H1B)<br />"
							+ "Dennis";

					String responseMessage = "What is the C2C rate if it's a contract?<br /> "
							+ "is it above $100/hr and remote? <br />"
							+ "Is it available for H1B holders? (I'm Canadian citizen and have a US H1B)<br />"
							+ "Dennis";

					responseMessage = modifyContent(content, responseMessage1, from, msgDate);

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

	private static boolean isSkippable(Message msg, String subject, String from, String toList, String ccList,
			Set<String> sentEmailAddresses) {
		boolean skippable = false;

		String[] fromIgnoreList = { "@stellarit.com", "@stansource.com", "@synechron.com", "@indeed.com",
				"calendar-notification@google.com", "kethireddy", "triplebyte" };
		String[] subjectIgnoreList = { "re:", "rtr", "triplebyte" };
		String[] contentIgnoreList = { "webex", "teams.microsoft", "meet.google.com", "zoom", "triplebyte" };

		try {
			skippable = false;
			for (String fi : fromIgnoreList) {
				if (from.endsWith(fi) || toList.contains(fi) || ccList.contains(fi)
						|| from.toLowerCase().contains(fi)) {
					System.out.println("Skipping this email because it's from " + fi + ". Subject=" + msg.getSubject());
					skippable = true;
					break;

				}
			}

			if (!skippable) {
				for (String sentEmailAddress : sentEmailAddresses) {
					if (from.endsWith(sentEmailAddress) || toList.contains(sentEmailAddress)
							|| ccList.contains(sentEmailAddress) || from.toLowerCase().contains(sentEmailAddress)) {
						System.out.println("Skipping this email because I have responded in the past 3 months to "
								+ sentEmailAddress + ". Subject=" + msg.getSubject());
						skippable = true;
						break;

					}
				}
			}

			if (!skippable) {
				for (String si : subjectIgnoreList) {

					if (subject.toLowerCase().contains(si)) {
						System.out.println("Skipping this email because it has " + si + " in subject. Subject="
								+ msg.getSubject());
						skippable = true;
						break;
					}

				}

			}

			if (!skippable) {

				String content = getContent(msg);

				String contentLowerCase = content.toLowerCase();
				for (String ci : contentIgnoreList) {

					if (contentLowerCase.contains(ci)) {
						System.out.println("Skipping this email because it has " + ci + " in content. Subject="
								+ msg.getSubject());
						skippable = true;
						break;
					}

				}

			}

		} catch (Exception e) {
			System.out.println(e);
		}

		return skippable;
	}

	private static boolean isDeleteable(Message msg, String subject, String from, String toList, String ccList) {

		boolean isDeletetable = false;

		try {

			// move this to a properties file
			String[] deleteKeywordsSubject = { "sap mm", "cpe test", "ui developer", ".net developer", "dot net lead",
					"voip", "php developer", "java fsd", "full-stack", "data integration", "information security",
					"camunda lead", "mobile architect", "firmware engineer", "dot net architect", "mulesoft",
					"sailpoint", "okta consultant", "business intelligence", "active directory", "validation engineer",
					"platform engineer", "jira admin", "content management", "oracle ebs", "mdm architect",
					".net technical", "datastage", "pen testing", "penetration", "triage", "lims developer",
					"sql developer", "sql software developer", "jira admin", "qlty assurance", "power platform",
					"golang", "systems administrator", "sap abap", "devops", "salesforce", "desktop support",
					"data architect", "snowflake", "oracle service bus", "service desk",
					"sql server database developer", "citizen only", "performance engineer",
					"performance test engineer", "c developer", "endeca", "blueyonder", "angular", ".net", "mulesoft",
					"mule soft", "automation", "big data", "business analyst", "etl developer", "new voicemail",
					"qa tech", "qa lead", "hadoop", "node.js", "nodejs", "ui engineer", "android", "data analyst",
					"etl", "data engineer", "embedded", "systems analyst", "system analyst", "quality analyst",
					"dotnet", "osb developer", "test analyst", "ux designer", "ui architect", "tester", "splunk",
					"new text message", "ui lead", "ui developer", "informatica", "front end", "azure",
					"ebs order management", "ios developer", "weblogic", "machine learning", "sap ", "pl/sql developer",
					"quality assurance", "oracle financials", "oracle app", "peoplesoft", "aws dev", "product manager",
					"operation manager", "splunk", "tableau", "c++", "data architect", "support", "ruby", "dynamics",
					"hl7", "qa analyst", "sdet", "mainframe", "tableau", ".net developer", "oracle soa", "c programmer",
					"android lead", "wireless", "python", "salesforce", "react", "qa analyst", "qa engineer",
					"full stack engineer", "cloud architect", "devops", "zuora", "guidewire", "test lead",
					"apache camel", "oracle dba", "bigdata", "osb", "manufacturing", "network", "cyberark", "flexera",
					"security engineer", "4gl", "sterling", "incorta", "ace developer", "oracle atg", "mobile test",
					"apache spark", "tibco", "modeler", "ormb", "wi-fi", "scrum master", "map reduce", "ml engineer",
					"websphere", "oracle epm", "pega"

			};

			String[] deleteKeywordsInMessage = { "no c2c", "us citizen only", "only us citizen", "no remote"

			};

			boolean keyWordFound = false;
			String keyWord = "";
			for (String d : deleteKeywordsSubject) {
				if (!d.isBlank() && subject.contains(d)) {
					System.out.println("delete keyword =" + d);
					keyWordFound = true;
					keyWord = d;
					break;
				}
			}
			String content = getContent(msg);

			String contentLowerCase = content.toLowerCase();
			for (String d : deleteKeywordsInMessage) {
				if (!d.isBlank() && contentLowerCase.contains(d)) {
					System.out.println("delete keyword =" + d);
					keyWordFound = true;
					keyWord = d;
					break;
				}
			}

			if (keyWordFound) {
				System.out.println("Deleting  this email keyword: " + keyWord + " from: " + from + " ccList: " + ccList
						+ "subject: " + msg.getSubject());

				isDeletetable = true;
			}

			String[] deleteEmailsFrom = { "donotreply@indeed.com", "notifications-noreply@linkedin.com",
					"jobalerts-noreply@linkedin.com", "invitations@linkedin.com", "messages-noreply@linkedin.com",
					"inmail-hit-reply@linkedin.com" };
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

	static Set<String> readSentEmailFile(String filePathEmailSent) {
		Set<String> sentEmailAddresses = new HashSet<String>();
		try {
			File f = new File(filePathEmailSent);
			if (!f.exists()) {
				System.out.println("file does not exist");
				return sentEmailAddresses;
			}
			// Create a date format
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
			// get the last modified date and format it to the defined format
			System.out.println("File last modified date " + sdf.format(f.lastModified()));
			Date currentTime = new Date();
			
			

			if (currentTime.getTime() - f.lastModified() < 24 * 60 * 60 * 1000) {// older than a day
				System.out.println("File is recent  " + sdf.format(f.lastModified()));
				BufferedReader reader = new BufferedReader(new FileReader(filePathEmailSent));

				String line = reader.readLine();
				while (line != null) {
					System.out.println(line);

					sentEmailAddresses.add(line);
					line = reader.readLine();

				}

				reader.close();
			} else {

				System.out.println("File is an old file " + sdf.format(f.lastModified()) + " hence not checking");

			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return sentEmailAddresses;
	}

	static void writeSentEmailFile(String filePathEmailSent, Set<String> sentEmailAddresses) {

		try {
			System.out.println("writing sent email addresses to file....");
			File fout = new File(filePathEmailSent);
			if (fout.exists()) {
				fout.delete();
				System.out.println("Deleted File " + filePathEmailSent);
			}

			fout = new File(filePathEmailSent);
			FileOutputStream fos = new FileOutputStream(fout);

			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

			for (int i = 0; i < sentEmailAddresses.size(); i++) {

				for (String sentEmailAddress : sentEmailAddresses) {
					bw.write(sentEmailAddress.trim());
					bw.newLine();

				}

			}

			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	static Set<String> getSentEmailAddresses(Store store) {

		Set<String> sentEmailAddresses = readSentEmailFile(filePathEmailSent);

		try {

			if (sentEmailAddresses.size() > 0) {
				return sentEmailAddresses;
			} else {

				Folder sent = store.getFolder("[Gmail]/Sent Mail");

				sent.open(Folder.READ_ONLY);

				Message[] sentMessages = sent.getMessages();
				System.out.println("sentMessages count=" + sentMessages.length);

				for (int i = sentMessages.length - 1; i > 0; i--) {

					Message msg = sentMessages[i];
					Date msgDate = msg.getReceivedDate();

					System.out.print(".");
					Date currentTime = new Date();
					if (currentTime.getTime() - msgDate.getTime() < daysInclude * 24 * 60 * 60 * 1000) {

						String toList = parseAddresses(msg.getRecipients(RecipientType.TO));
						String ccList = parseAddresses(msg.getRecipients(RecipientType.CC));

						Matcher matcher = Pattern.compile("[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}")
								.matcher(" " + toList + "  " + ccList);

						while (matcher.find()) {
							sentEmailAddresses.add(matcher.group());
							System.out.println("Added " + matcher.group());
						}

						// System.out.println(msgDate.toLocaleString() + "toList=" + toList + " ccList="
						// + ccList);

						// System.out.println("Skipping this email because its recent. subject=" +
						// msg.getSubject());
						// continue;
					} else {
						System.out.println("Exiting loop ");
						break;
					}

				}

				writeSentEmailFile(filePathEmailSent, sentEmailAddresses);
			}

		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return sentEmailAddresses;

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
