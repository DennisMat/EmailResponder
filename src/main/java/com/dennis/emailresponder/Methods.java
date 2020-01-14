package com.dennis.emailresponder;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Date;
import java.util.Properties;    
import javax.mail.*;    
import javax.mail.internet.*;
import javax.mail.search.FlagTerm;

import com.sun.mail.imap.protocol.FLAGS;    
class Methods{  



	static void send(final String email,final String password,
			String to,String sub,String msg,Session session ){  

		Authenticator auth = new javax.mail.Authenticator() {    
			protected PasswordAuthentication getPasswordAuthentication() {    
				return new PasswordAuthentication(email,password);  
			}    
		}; 

		Properties props = setAndGetProperties(); 
		session = Session.getInstance(props, auth);



		//compose message    
		try {    
			MimeMessage message = new MimeMessage(session);    
			message.addRecipient(Message.RecipientType.TO,new InternetAddress(to));    
			message.setSubject(sub);    
			message.setText(msg, "utf-8", "html");    
			//send message  
			Transport.send(message);    
			System.out.println("Message sent successfully to: " + to);    
		} catch (MessagingException e) {throw new RuntimeException(e);}    

	}




	static void listUnreadEmails(final String email,final String password, final String host) {    	    	
		//Properties props = System.getProperties();
		Properties props = setAndGetProperties(); 

		props.setProperty("mail.store.protocol", "imaps");
		Session session = Session.getDefaultInstance(props, null);

		try {
			Store store = session.getStore("imaps");
			store.connect(host, email, password);

			Folder inbox = store.getFolder("INBOX");
			inbox.open(Folder.READ_WRITE);


			Flags seen = new Flags(Flags.Flag.SEEN);
			FlagTerm unseenFlagTerm = new FlagTerm(seen, false);
			Message messages[] = inbox.search(unseenFlagTerm);



			for (Message msg : messages) {

				String subject =msg.getSubject().toLowerCase();
				String contentType = msg.getContentType().toLowerCase();
				String content=null;
				String from = ((InternetAddress)msg.getFrom()[0]).getAddress();
				Date date=msg.getReceivedDate();


				if(subject.contains("angular") || subject.contains(".net")
						|| 	subject.contains("mulesoft") || subject.contains("mule soft")
						|| 	subject.contains("automation") || subject.contains("big data")
						|| 	subject.contains("business analyst") || subject.contains("etl developer")

						) {
					System.out.println(" deleting this email subject=" + subject);
					msg.setFlag(FLAGS.Flag.DELETED, true);
					continue;
				}

				if(subject.startsWith("re:")) {
					System.out.println("1 skipping this email subject=" + subject);
					continue;
				}

				if(!subject.contains("java")) {
					System.out.println("2 skipping this email subject=" + subject);
					continue;
				}

				if((subject.contains("full time") || subject.contains("fulltime") )&& !subject.contains("contract")) {
					System.out.println("3 skipping this email subject=" + subject);
					continue;
				}




				if (contentType.contains("multipart")) {
					// content may contain attachments
					// javax.mail.internet.MimeMultipart multiPart =    (javax.mail.internet.MimeMultipart) message.getContent();
					Multipart multiPart = (Multipart) msg.getContent();

					int numberOfParts = multiPart.getCount();
					for (int partCount = 0; partCount < numberOfParts; partCount++) {
						MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(partCount);
						if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {

						} else {
							// this part may be the message content
							// content = part.getContent().toString();
							content = dumpPart(part);
							int a=3;
						}
					}


				} else if (contentType.contains("text/plain")
						|| contentType.contains("text/html")) {
					Object ob = msg.getContent();
					if (ob != null) {
						content = ob.toString(); 
					}
				}

				System.out.println("will respond to Subject = " + msg.getSubject());
				//System.out.println("Content = " + content);

				boolean isHighCost=false;




				String responseMessage="Is C2C option available for this position?<br />"
						+ "Is the hourly rate more that 115$/hr, C2C?<br />"
						+"Dennis";

				responseMessage = modifyContent(content, responseMessage, from, date);

				send(email, password,from,"Re: "+msg.getSubject(), responseMessage,session);


				//inbox.close(false);

			}
		}catch(Exception e)    {
			System.out.println(e);
		}

	}

	static Properties setAndGetProperties() {
		//Get properties object    
		Properties props = new Properties();    
		props.put("mail.smtp.host", "smtp.gmail.com");    
		props.put("mail.smtp.socketFactory.port", "465");    
		props.put("mail.smtp.socketFactory.class",    
				"javax.net.ssl.SSLSocketFactory");    
		props.put("mail.smtp.auth", "true");    
		props.put("mail.smtp.port", "465");
		return props;
	}  


	/*
	 * Not sure if this is working, but this method may never be never touched.
	 */
	static String  dumpPart(Part p) throws Exception {
		String contentType = p.getContentType();
		System.out.println("dumpPart" + contentType);
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

	static String  modifyContent(String content,String responseMessage, String from, Date date) throws Exception {


		return responseMessage + "<br /><br />" + date.toString() + " <"+ from +  "> <br />" +  content;
	}


}  
