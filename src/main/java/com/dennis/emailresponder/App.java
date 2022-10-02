package com.dennis.emailresponder;

/**
 * Hello world!
 *
 */
public class App {

	static String email = "dennis.mathew2000@gmail.com";
	static String password = "xxxxxxxxxxx";
	static String host = "imap.gmail.com";

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
}
