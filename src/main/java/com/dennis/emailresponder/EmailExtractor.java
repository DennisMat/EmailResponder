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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Store;
import javax.mail.Message.RecipientType;
import javax.mail.internet.MimeBodyPart;

public class EmailExtractor {
	

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

			if (currentTime.getTime() - f.lastModified() < App.fileAgeInDays * 24 * 60 * 60 * 1000) {// older than a
																									// fileAgeInDays
																									// days
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

		Set<String> sentEmailAddresses = new HashSet<String>();

		try {
			Set<String> sentEmailAddressesfromFile = readSentEmailFile(App.filePathEmailSent);

			sentEmailAddresses.addAll(sentEmailAddressesfromFile);
			File f = new File(App.filePathEmailSent);
			if (!f.exists()) {
				System.out.println("file does not exist");

			}
			// Create a date format
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
			// get the last modified date and format it to the defined format
			System.out.println("File last modified date " + sdf.format(f.lastModified()));
			Date currentTime = new Date();

			Set<String> recentSentEmailAddresses = extractEmailAddresses(store, f.lastModified() + 2);

			sentEmailAddresses.addAll(recentSentEmailAddresses);
			writeSentEmailFile(App.filePathEmailSent, sentEmailAddresses);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return sentEmailAddresses;

	}

	private static Set<String> extractEmailAddresses(Store store, long getUpto) throws MessagingException {
		Set<String> sentEmailAddresses = new HashSet<String>();

		Folder sent = store.getFolder("[Gmail]/Sent Mail");

		sent.open(Folder.READ_ONLY);

		Message[] sentMessages = sent.getMessages();
		System.out.println("sentMessages count=" + sentMessages.length);

		String[] sentbyMe = { "On Sun,", "On Mon,", "On Tue,", "On Wed,", "On Thu,", "On Fri,", "On Sat," };

		for (int i = sentMessages.length - 1; i > 0; i--) {

			Message msg = sentMessages[i];
			Date msgDate = msg.getReceivedDate();

			Date currentTime = new Date();
			if (msgDate.getTime() > getUpto) {

				String toList = parseAddresses(msg.getRecipients(RecipientType.TO));
				String ccList = parseAddresses(msg.getRecipients(RecipientType.CC));

				Matcher matcher = Pattern.compile("[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}")
						.matcher(" " + toList + "  " + ccList);

				while (matcher.find()) {

					for (int j = 0; j < sentbyMe.length; j++) {
						String content = getContent(msg);
						if (content.contains(sentbyMe[j])) {
							sentEmailAddresses.add(matcher.group());
							System.out.println("Added " + matcher.group());
							break;
						}
					}

				}

			} else {
				System.out.println("Exiting loop ");
				break;
			}

		}

		return sentEmailAddresses;
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

	static void softDelete(Folder inbox, Folder trash, Message msg) throws MessagingException {
		Message[] msgArr = new Message[1];
		msgArr[0] = msg;
		inbox.copyMessages(msgArr, trash);
		// This line is not needed the above line will handle it
		// msg.setFlag(FLAGS.Flag.DELETED, true);
	}

	static String getContent(Message msg) {

		String content = null;
		try {
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
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return content;
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


}
