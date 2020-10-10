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

import com.sun.mail.imap.protocol.FLAGS;    
class Methods{  


	static void processUnreadEmails(String email, String password, String host){
		
		int minutes = 10;
		try {
			//while(true) {
			scanUnreadEmails(email, password, host);
			//Thread.sleep(1000*60*minutes);
			//}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	

	static void scanUnreadEmails(String email, String password, String host) {
		System.out.println("\n\nNow scanning emails...\n");
		//Properties props = System.getProperties();
		Properties props = setAndGetProperties(); 

		props.setProperty("mail.store.protocol", "imaps");
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

			int count=0;

			for (Message msg : messages) {
				System.out.println("\n\n------------Message # " + ++count + "----------------");
				String subject =msg.getSubject().toLowerCase();
				
			
				String from = ((InternetAddress)msg.getFrom()[0]).getAddress();
				String toList = parseAddresses(msg
                        .getRecipients(RecipientType.TO));
                String ccList = parseAddresses(msg
                        .getRecipients(RecipientType.CC));
                
				Date date=msg.getReceivedDate();

				if(from.endsWith("@stellarit.com") || toList.contains("@stellarit.com")
						|| ccList.contains("@stellarit.com")){					
					System.out.println("Skipping 0 this email subject=" + msg.getSubject());
					continue;
				}
				
				if(subject.startsWith("re:")) {
					System.out.println("Skipping 1 this email subject=" + msg.getSubject());
					continue;
				}
				
				//move this to a properties file
				String[] delteKeywords= {
						"angular",".net","mulesoft","mule soft","automation","big data","business analyst","etl developer","new voicemail","qa tech",
						"qa lead","hadoop","node.js","ui engineer","android","data analyst","etl","data engineer","embedded",
						"systems analyst","system analyst","quality analyst","dotnet","osb developer","test analyst","ux designer","ui architect",
						"tester","splunk","new text message","ui lead","informatica","front end","azure",
						"ebs order management","ios developer","weblogic","machine learning","sap ","pl/sql developer", "quality assurance",
						"oracle financials","peoplesoft", "aws dev", "product manager","operation manager","splunk"
				
				};
				
				boolean keyWordFound=false;
				String keyWord="";
				for (String d : delteKeywords) {
					if(subject.contains(d)){
						keyWordFound=true;
						keyWord=d;
						break;
					}
			}
				
				
				if(keyWordFound) {
					System.out.println("Deleting 2 this email keyword: "+keyWord+"from: " 
							+ from + " ccList: " + ccList + "subject: " + msg.getSubject());						
						softDelete(inbox, trash, msg);		
						continue;
				}



				if(!subject.contains("java")) {
					System.out.println("Skipping 2 this email subject=" + msg.getSubject());
					continue;
				}

				if((subject.contains("full time") || subject.contains("permanent")
					||	subject.contains("fulltime") || subject.contains("fte") )
						
						&& !(subject.contains("contract") || subject.contains("c2c") || subject.contains("cwr")  )) {
					System.out.println("Deleting 3 this email from: " 
							+ from + " ccList: " + ccList + "subject: " + msg.getSubject());
					softDelete(inbox, trash, msg);
					continue;
				}
				
				if(subject.contains("w2 only")) {
					System.out.println("Skipping 4 this email subject=" + msg.getSubject());
					softDelete(inbox, trash, msg);
					continue;
				}

			

				String content = getContent(msg);

				String contentLowerCase=content.toLowerCase();
				
				
				if(contentLowerCase.contains("fte only") || contentLowerCase.contains("no c2c")) {
					System.out.println("Skipping 5 this email subject=" + msg.getSubject());
					continue;
				}
				

				if(contentLowerCase.contains("contract") || contentLowerCase.contains("duration")
					|| contentLowerCase.contains("long term") || contentLowerCase.contains("months")
						) {

					if(contentLowerCase.contains("java")) {

						System.out.println("will respond to Subject = " + msg.getSubject());
						//System.out.println("Content = " + content);

						String responseMessage="Is C2C option available for this position?<br />"
								+"Is it avaialble for H1B holders(I'm Canadian citizen and have a US H1B)<br />"
								+ "What are the best hourly rates, C2C?<br />"
								+"Dennis";

						responseMessage = modifyContent(content, responseMessage, from, date);

						boolean isSuccess= send(email, password,from,"Re: "+msg.getSubject(), responseMessage,session);

						if(isSuccess) {
							System.out.println("Deleting this email after responding. subject = " + msg.getSubject());					
							softDelete(inbox, trash, msg);
						}
							
							
							
							

					}

					//inbox.close(false);
				}else{
					System.out.println("Skipping 5 this email subject=" + msg.getSubject());
					continue;
				}



			}
		}catch(Exception e)    {
			System.out.println(e);
		}

		System.out.println("\nAll emails processed\n");
	}



	static void softDelete(Folder inbox, Folder trash, Message msg) throws MessagingException {
		Message[] msgArr = new Message[1];
		msgArr[0]=msg;
		inbox.copyMessages(msgArr, trash);
		//This  line is not needed the above line will handle it
		//msg.setFlag(FLAGS.Flag.DELETED, true); 
	}



	static String getContent(Message msg)
			throws IOException, MessagingException, Exception {
		
		String content=null;
		String contentType = msg.getContentType().toLowerCase();
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
		return content;
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
		//System.out.println("dumpPart" + contentType);
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


		return responseMessage + "<br /><br />" + date.toString() + " &lt;"+ from +  "&gt; wrote: <br />" +  content;
	}



	static boolean send(final String email,final String password,
			String to,String sub,String msg,Session session ){  
		boolean isSuccess=false;
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
			isSuccess=true;
		} catch (MessagingException e) {throw new RuntimeException(e);}    

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
