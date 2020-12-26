
import java.util.*;
import java.net.*;
import java.text.*;
import java.lang.*;
import java.io.*;
import java.sql.*;
import pgpass.*;

import java.util.Date;
import java.util.Calendar;
//import project4.PgPassException;

public class CreateQuest {

	private Connection conDB; // Connection to the database system.
	private String url; // URL: Which database?
	private String user = "alexmf99"; // Database user account
	private int SQL_TOTAL = 0;
	private Integer custID; // Who are we tallying?
	private String custName; // Name of that customer.

	public CreateQuest(String[] args) {

		try {
			// Register the driver with DriverManager.
			Class.forName("org.postgresql.Driver").newInstance();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(0);
		} catch (InstantiationException e) {
			e.printStackTrace();
			System.exit(0);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			System.exit(0);
		}

		// URL: Which database?
		// url = "jdbc:postgresql://db:5432/<dbname>?currentSchema=yrb";
		url = "jdbc:postgresql://db:5432/";

		// set up acct info
		// fetch the PASSWD from <.pgpass>
		Properties props = new Properties();
		try {
			String passwd = PgPass.get("db", "*", user, user);
			if (args.length >= 5) {
				props.setProperty("user", user);

			} else {

				props.setProperty("user", "alexmf99");

			}

			props.setProperty("password", passwd);
			// props.setProperty("ssl","true"); // NOT SUPPORTED on DB
		} catch (PgPassException e) {
			System.out.print("\nCould not obtain PASSWD from <.pgpass>.\n");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Initialize the connection.
		try {
			// Connect with a fall-thru id & password
			// conDB = DriverManager.getConnection(url,"<username>","<password>");
			conDB = DriverManager.getConnection(url, props);
		} catch (SQLException e) {
			System.out.print("\nSQL: database connection error.\n");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Let's have autocommit turned off. No particular reason here.
		try {
			conDB.setAutoCommit(false);
		} catch (SQLException e) {
			System.out.print("\nFailed trying to turn autocommit off.\n");
			e.printStackTrace();
			System.exit(0);
		}

		// Set to user or default if no user provided

		// handle different error cases

		// check all parameter for valid inputs

		try {
			if (!dayIsInFuture(args[0])) {
				System.out.print("Day " + args[0] + " is not in the future\n");
				System.exit(0);

			}

			if (!realmExist(args[1])) {

				System.out.print("Realm " + args[1] + " does not exist\n");
				System.exit(0);

			}

			if (exceeds(Integer.valueOf(args[3]))) {

				System.out.print("Amount " + args[2] + " exceeds the sql amount requested\n");
				System.exit(0);
			}

			// checking for valid seed if argument

			if (args.length == 6) {

				if (!validSeed(args[5])) {

					System.out.print("Seed " + args[5] + " is not between [-1,1]\n");
					System.exit(0);

				}

			}

			// we are done checking all valid cases

			// insert into DB Quest and Loot associated with that quest
			insertQuest(args);
			assignLoot(args);

		} catch (Exception e) {

		}

		try {
			conDB.commit();
		} catch (SQLException e) {
			System.out.print("\nFailed trying to commit.\n");
			e.printStackTrace();
			System.exit(0);
		}

	}

	void insertQuest(String args[]) {

		String queryText = ""; // The SQL text.
		PreparedStatement querySt = null; // The query handle.
		ResultSet answers = null; // A cursor.
		String theme = args[2];
		String day = args[0];
		String realm = args[1];
		java.sql.Date sql = null;
		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
			Date parsed = format.parse(day);
			sql = new java.sql.Date(parsed.getTime());

		} catch (Exception e) {

			System.out.println("FAILED parsing SQL DATE\n");
		}
		queryText = "INSERT INTO Quest VALUES(?,?,?)";

		// Prepare the query.
		try {
			querySt = conDB.prepareStatement(queryText);
		} catch (SQLException e) {
			System.out.println("SQL#1 failed in prepare");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Execute the query.
		try {

			querySt.setString(1, theme);
			querySt.setString(2, realm);
			querySt.setDate(3, sql);
			querySt.executeUpdate();
		} catch (SQLException e) {
			System.out.println("SQL#1 failed in execute");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Any answer?

		// Close the cursor.
		try {
			answers.close();
		} catch (SQLException e) {
			System.out.print("SQL#1 failed closing cursor.\n");
			System.out.println(e.toString());
			System.exit(0);
		}

		// We're done with the handle.
		try {
			querySt.close();
		} catch (SQLException e) {
			System.out.print("SQL#1 failed closing the handle.\n");
			System.out.println(e.toString());
			System.exit(0);
		}
		
	//removed commit here

	}

	void setSeed(String seed) {
		String queryText = ""; // The SQL text.

		PreparedStatement querySt = null; // The query handle.
		ResultSet answers = null; // A cursor.

		queryText = "SELECT setseed(?)";

		try {
			querySt = conDB.prepareStatement(queryText);
		} catch (SQLException e) {
			System.out.println("SQL#1 failed in prepare");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Execute the query.
		try {
			querySt.setInt(1, Integer.valueOf(seed));
			answers = querySt.executeQuery();
		} catch (SQLException e) {
			System.out.println("SQL#1 failed in execute");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Close the cursor.
		try {
			answers.close();
		} catch (SQLException e) {
			System.out.print("SQL#1 failed closing cursor.\n");
			System.out.println(e.toString());
			System.exit(0);
		}

		// We're done with the handle.
		try {
			querySt.close();
		} catch (SQLException e) {
			System.out.print("SQL#1 failed closing the handle.\n");
			System.out.println(e.toString());
			System.exit(0);
		}

	}

	// Returns reference to generated table of random treasures
	ResultSet generateTreasure() {
		
		String queryText = ""; // The SQL text.

		PreparedStatement querySt = null; // The query handle.
		ResultSet answers = null; // A cursor and reference to table.

		queryText = "SELECT * FROM Treasure" + " order by random()";
		// Prepare the query.
		try {
			querySt = conDB.prepareStatement(queryText);
		} catch (SQLException e) {
			System.out.println("SQL#1 failed in prepare");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Execute the query.
		try {

			answers = querySt.executeQuery();
		} catch (SQLException e) {
			System.out.println("SQL#1 failed in execute");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Close the cursor.

		return answers;

	}

	void assignLoot(String[] args) throws SQLException {

		String theme = args[2];
		String realm = args[1];
		String day = args[0];

		ResultSet treasureTable = null;

		java.sql.Date sql = null;
		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
			Date parsed = format.parse(day);
			sql = new java.sql.Date(parsed.getTime());

		} catch (Exception e) {

			System.out.println("failed pasring date\n");
		}

		treasureTable = generateTreasure();

		ArrayList<String> temp = new ArrayList<String>();
		int amount = Integer.valueOf(args[3]);

		while (treasureTable.next() && amount >= 0) {

			temp.add(treasureTable.getString("treasure"));
			amount -= treasureTable.getInt("sql");

		}

		for (int i = 0; i < temp.size(); i++) {

			insertLoot(i + 1, temp.get(i), theme, realm, sql);

		}

	}

	void insertLoot(int id, String treasure, String theme, String realm, java.sql.Date date) {
ResultSet answers = null;
		String queryText = "insert into Loot VALUES(?,?,?,?,?,null)";
		PreparedStatement querySt = null; // The query handle.

		// Prepare the query.
		try {
			querySt = conDB.prepareStatement(queryText);
		} catch (SQLException e) {
			System.out.println("SQL#1 failed in prepare");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Execute the query.
		try {
			querySt.setInt(1, id);
			querySt.setString(2, treasure);
			querySt.setString(3, theme);
			querySt.setString(4, realm);
			querySt.setDate(5, date);

			answers = querySt.executeQuery();
		} catch (SQLException e) {
			System.out.println("SQL#1 failed in execute");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Any answer?

		
		try {
			answers.close();
		} catch (SQLException e) {
			System.out.print("SQL#1 failed closing cursor.\n");
			System.out.println(e.toString());
			System.exit(0);
		}

		// We're done with the handle.
		try {
			querySt.close();
		} catch (SQLException e) {
			System.out.print("SQL#1 failed closing the handle.\n");
			System.out.println(e.toString());
			System.exit(0);
		}
		
		
		try {
			conDB.commit();
		} catch (SQLException e) {
			System.out.print("\nFailed trying to commit.\n");
			e.printStackTrace();
			System.exit(0);
		}



	}

	boolean exceeds(int loot) {

		String queryText = ""; // The SQL text.
		PreparedStatement querySt = null; // The query handle.
		ResultSet answers = null; // A cursor.

		boolean lootExceeded = true; // Return.

		queryText = "SELECT sum(T.sql) as total from Treasure as T";
		// Prepare the query.
		try {
			querySt = conDB.prepareStatement(queryText);
		} catch (SQLException e) {
			System.out.println("SQL#1 failed in prepare");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Execute the query.
		try {

			answers = querySt.executeQuery();
		} catch (SQLException e) {
			System.out.println("SQL#1 failed in execute");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Any answer?
		try {
			if (answers.next()) {
				// total = Integer.valueOf(answers.getString("seed"));

				lootExceeded = answers.getInt("total") < loot;
				SQL_TOTAL = answers.getInt("total");

			}
		} catch (SQLException e) {
			System.out.println("SQL#1 failed in cursor.");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Close the cursor.
		try {
			answers.close();
		} catch (SQLException e) {
			System.out.print("SQL#1 failed closing cursor.\n");
			System.out.println(e.toString());
			System.exit(0);
		}

		// We're done with the handle.
		try {
			querySt.close();
		} catch (SQLException e) {
			System.out.print("SQL#1 failed closing the handle.\n");
			System.out.println(e.toString());
			System.exit(0);
		}

		return lootExceeded;
	}

	boolean validSeed(String seed) {

		Float num;
		boolean seedInRange = false;

		try {
			num = Float.parseFloat(seed);

			if (num < 1 && num > -1) {

				seedInRange = true;

			}

		}

		catch (Exception e) {

		}

		return seedInRange;
	}

	boolean realmExist(String realm) {

		String queryText = ""; // The SQL text.
		PreparedStatement querySt = null; // The query handle.
		ResultSet answers = null; // A cursor.

		boolean realmInDB = false; // Return.

		queryText = "select * from Realm where exists(select 1 from Realm where realm = ?)";

		// Prepare the query.
		try {
			querySt = conDB.prepareStatement(queryText);
		} catch (SQLException e) {
			System.out.println("SQL#1 failed in prepare");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Execute the query.
		try {
			querySt.setString(1, realm);
			// querySt.setInt(1, custID.intValue());
			answers = querySt.executeQuery();
		} catch (SQLException e) {
			System.out.println("SQL#1 failed in execute");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Any answer?
		try {
			if (answers.next()) {
				realmInDB = true;

			} else {
				realmInDB = false;

			}
		} catch (SQLException e) {
			System.out.println("SQL#1 failed in cursor.");
			System.out.println(e.toString());
			System.exit(0);
		}

		// Close the cursor.
		try {
			answers.close();
		} catch (SQLException e) {
			System.out.print("SQL#1 failed closing cursor.\n");
			System.out.println(e.toString());
			System.exit(0);
		}

		// We're done with the handle.
		try {
			querySt.close();
		} catch (SQLException e) {
			System.out.print("SQL#1 failed closing the handle.\n");
			System.out.println(e.toString());
			System.exit(0);
		}

		return realmInDB;

	}

	boolean dayIsInFuture(String day) {

		SimpleDateFormat sdformat = new SimpleDateFormat("yyyy-MM-dd");
		Date userInput = null;
		try {
			userInput = sdformat.parse(day);

		}

		catch (Exception e) {
		}

		Date currentDate = new Date();

		if (currentDate.compareTo(userInput) < 0)
			return true;

		return false;

	}

	public static void main(String[] args) {
		CreateQuest ct = new CreateQuest(args);
	}

}
