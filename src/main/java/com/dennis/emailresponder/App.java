package com.dennis.emailresponder;

/**
 * Hello world!
 *
 */
public class App 
{
	
	static String email="dennis.mathew2000@gmail.com";
	static String password="gdientag";
	static String host="imap.gmail.com";
	
	/*
	 
	 Too low, looking for at least $110/hr for California.Or $100/hr if remote.
	 
	 */
	
    public static void main( String[] args )
    {
    	Methods.processUnreadEmails(email, password, host);
    	
    }
}
