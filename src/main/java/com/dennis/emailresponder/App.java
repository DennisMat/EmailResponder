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
    	Methods.processUnreadEmails(email, password, host);
    	
    }
}
