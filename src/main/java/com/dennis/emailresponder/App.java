package com.dennis.emailresponder;

/**
 * Hello world!
 *
 */
public class App {

	static String email = "dennis.mathew2000@gmail.com";
	static String password = "msnkzbwlfodhwqmp";
	static String host = "imap.gmail.com";
	static int minutesSkip = 2;// skip emails that came in the last 10 minutes;

	static long daysInclude = 90;// 3 months
	static long fileAgeInDays = 5;
	static String filePathEmailSent = "C:\\temp\\SentEmails.txt";

	public static void main(String[] args){
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

}
