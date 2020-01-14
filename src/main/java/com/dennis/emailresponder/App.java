package com.dennis.emailresponder;

/**
 * Hello world!
 *
 */
public class App 
{
	
	static String email="dennis.mathew2000@gmail.com";
	static String password="xxxxxx";
	static String host="imap.gmail.com";
	
	
	
    public static void main( String[] args )
    {
    	  //from,password,to,subject,message  
    	Methods.listUnreadEmails(email, password, host);
    	
       // Mailer.send("from@gmail.com","xxxxx","to@gmail.com","hello javatpoint","How r u?");  
        //change from, password and to  
    }
}
