package com.dennis.emailresponder;


import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.mail.*;
import javax.mail.Message.RecipientType;
import javax.mail.internet.*;
import javax.mail.search.FlagTerm;

class Methods {


	static void processUnreadEmails(String email, String password, String host) {

		int minutes = 10;
		try {
			// while(true) {
			Properties props = setAndGetProperties();
			scanUnreadEmails(email, password, host,props);
			// Thread.sleep(1000*60*minutes);
			// }
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	static void scanUnreadEmails(String email, String password, String host, Properties props) {
		System.out.println("\n\nNow scanning emails...\n");
		

		Session session = Session.getDefaultInstance(props, null);

		try {
			Store store = session.getStore("imaps");
			store.connect(host, email, password);

			//Set<String> sentEmailAddresses = getSentEmailAddresses(store);
			Set<String> sentEmailAddresses = new HashSet<String>();

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
				String toList = EmailExtractor.parseAddresses(msg.getRecipients(RecipientType.TO));
				String ccList = EmailExtractor.parseAddresses(msg.getRecipients(RecipientType.CC));

				Date msgDate = msg.getReceivedDate();

				Date currentTime = new Date();

				if (currentTime.getTime() - msgDate.getTime() < App.minutesSkip * 60 * 1000) {
					System.out.println("Skipping this email because its recent. subject=" + msg.getSubject());
					continue;
				}

				boolean skippable = Rules.isSkippable(msg, subject, from, toList, ccList, sentEmailAddresses);

				if (skippable) {
					continue;
				}

				boolean isDelete = Rules.isDeleteable(msg, subject, from, toList, ccList);

				if (isDelete) {
					EmailExtractor.softDelete(inbox, trash, msg);
					continue;
				}

				if (!subject.contains("java")) {
					System.out.println("Skipping this email subject=" + msg.getSubject());
					continue;
				}

				String content = EmailExtractor.getContent(msg);

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

					responseMessage = modifyContent(content, responseMessage2, from, msgDate);

					boolean isSuccess = send(email, password, from, "Re: " + msg.getSubject(), responseMessage,
							session);

					if (isSuccess) {
						System.out.println("Deleting this email after responding. subject = " + msg.getSubject());
						EmailExtractor.softDelete(inbox, trash, msg);
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



}
