package com.dennis.emailresponder;

import java.util.Set;

import javax.mail.Address;
import javax.mail.Message;

public class Rules {
	
	static boolean isSkippable(Message msg, String subject, String from, String toList, String ccList,
			Set<String> sentEmailAddresses) {
		boolean skippable = false;

		String[] fromIgnoreList = { "@stellarit.com", "@stansource.com", 
				"@synechron.com", "@indeed.com","@ketsoftware.com",
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

				String content = EmailExtractor.getContent(msg);

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

	static boolean isDeleteable(Message msg, String subject, String from, String toList, String ccList) {

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
			String content = EmailExtractor.getContent(msg);

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

	


}
