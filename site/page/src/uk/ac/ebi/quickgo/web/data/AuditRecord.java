package uk.ac.ebi.quickgo.web.data;

public class AuditRecord {
	public enum AuditAction {
		A("Added"),
		U("Updated"),
		D("Deleted"),
		X("Unknown");

	    public String description;

	    AuditAction(String description) {
	        this.description = description;
	    }

		public static AuditAction fromString(String s) {
			if ("Added".equalsIgnoreCase(s)) {
				return A;
			}
			else if ("Updated".equalsIgnoreCase(s)) {
				return U;
			}
			else if ("Deleted".equalsIgnoreCase(s)) {
				return D;
			}
			else {
				return X;
			}
		}
	}

	public String goId;
	public String term;
	public String timestamp;
	public AuditAction action;
	public String category;
	public String text;

	public AuditRecord(String goId, String term, String timestamp, String action, String category, String text) {
		this.goId = goId;
		this.term = term;
		this.timestamp = timestamp;
		this.action = AuditAction.fromString(action);
		this.category = category.intern();
		this.text = text;
	}

	public boolean isA(String[] categories) {
		for (String cat : categories) {
			if (category.equals(cat)) {
				return true;
			}
		}
		return false;
	}

	public boolean isA(String category) {
		return category.equals(this.category);
	}

	public String action() {
		return action.description;
	}

	@Override
	public String toString() {
		return "AuditRecord{" +
				"goId='" + goId + '\'' +
				", term='" + term + '\'' +
				", timestamp='" + timestamp + '\'' +
				", action=" + action +
				", category='" + category + '\'' +
				", text='" + text + '\'' +
				'}';
	}
}
