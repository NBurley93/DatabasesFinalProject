/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.sql.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/**
 *
 * @author Original source: Nicholas J Burley
 * @author Adaption to Oracle: Andrew Sudduth
 */
public class AdminClient {
    public static void main(String[] args) {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            //Oracle connection
            String db_username="oracle_username", password = "oracle_password";
            DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
            c = DriverManager.getConnection (
                   "jdbc:oracle:thin:@oracle1.wiu.edu:1521/toolman.wiu.edu",
		             db_username, password);
            c.setAutoCommit(false);
            
            //Login
            System.out.print("Enter a valid admin login (ex. admin): ");
            String usr = br.readLine();
            while(attemptLogin(usr) == false) {
                //Login faliure
                System.out.print("Failed to validate admin credentials, please try again: ");
                usr = br.readLine();
            }      
            
            //Menu loop
            String select = "-1";
            String subselect = "-1";
            while(!select.equals("0")) {
                //Display main menu
                System.out.println("\nWelcome " + usr + "!");
                printMainMenu();
                select = br.readLine();
                
                //Sub-menus
                switch(select) {
                    case "1":
                        while(!subselect.equals("0")) {
                            //Display the user menu
                            printUserMenu();
                            subselect = br.readLine();
                            userMenu(subselect);
                        }
                        break;
                        
                    case "2":
                        while(!subselect.equals("0")) {
                            //Display the rank menu
                            printRankMenu();
                            subselect = br.readLine();
                            rankMenu(subselect);
                        }                        
                        break;
                        
                    case "3":
                        while(!subselect.equals("0")) {
                            //Display the group menu
                            printGroupMenu();
                            subselect = br.readLine();
                            groupMenu(subselect);
                        }                        
                        break;
                        
                    case "4":
                        while(!subselect.equals("0")) {
                            //Display the challenge menu
                            printChallengeMenu();
                            subselect = br.readLine();
                            challengeMenu(subselect);
                        }                        
                        break;
                }
                subselect = "-1";
            }
                        
            c.close(); //We're done, close the connection
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        
    }
    
    public static void createUser(String username, String RealName, String Email, String type) {
        //Utilize SQL here to insert a user into users
        try {
            //First check to see if the user already exists
            ResultSet r;
            stmt = c.createStatement();
            r = stmt.executeQuery("SELECT 1 FROM users WHERE username='" + username + "'");
            if (r.next()) {
                System.out.println(username + " already exists, please select a different username!");
                return;
            }
            
            //Continue otherwise
            stmt = c.createStatement();
            stmt.executeUpdate("INSERT INTO users (username,name,email,rankname,xp,type) VALUES ('" + username + "','" + RealName + "','" + Email + "','noob',0,'" + type + "')");
            System.out.println("Created new user (" + username + ")");
            c.commit();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    public static void removeUser(String username) {
        //Utilize SQL here to fetch a user, and remove them from the users table
        try {
            //Verify that the user exists
            ResultSet r;
            stmt = c.createStatement();
            r = stmt.executeQuery("SELECT 1 FROM users WHERE username='" + username + "'");
            if (r.next()) {
                //We first need to remove all dependencies for this user
                
                //First we'll delete the user's messages and groups
                stmt = c.createStatement();
                stmt.executeUpdate("DELETE FROM messages WHERE creator='" + username + "'");
                
                stmt = c.createStatement();
                r = stmt.executeQuery("SELECT groupname FROM groups WHERE groupleader='" + username + "'");
                if (r.next()) {
                    //Owns a group
                    removeGroup(r.getString("groupname"));
                }
                
                //Now take care of groups that they're members of
                stmt = c.createStatement();
                stmt.executeUpdate("DELETE FROM groupMembers WHERE username='" + username + "'");
                
                //Now remove their completed challenges
                stmt = c.createStatement();
                stmt.executeUpdate("DELETE FROM completedchallenges WHERE username='" + username + "'");
                
                //Now finally remove the user itself
                stmt = c.createStatement();
                stmt.executeUpdate("DELETE FROM users WHERE username='" + username + "'");
                System.out.println("Removed user: " + username);
                c.commit();
            } else {
                System.out.println("Cannot remove " + username + " as no user with that name exists");
                return;
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    public static void createChallenge(String challengeName, int requiredXP, String challengeDesc) {
        //Utilize SQL to create a challenge with the xp required
        try {
            //First confirm that the challenge does not exist already
            ResultSet r;
            stmt = c.createStatement();
            r = stmt.executeQuery("SELECT 1 FROM challenges WHERE challengename='" + challengeName + "'");
            if (r.next()) {
                //Already exists
                System.out.println(challengeName + " already exists, remove it first, then re-add if you want to modify it");
                return;
            }
            
            //Add the challenge
            stmt = c.createStatement();
            stmt.executeUpdate("INSERT INTO challenges (challengename,xpreq,challengedesc) VALUES ('" + challengeName + "'," + Integer.toString(requiredXP) + ",'" + challengeDesc + "')");
            c.commit();
            System.out.println("Created " + challengeName + " as new challenge!");
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    public static void removeChallenge(String challengeName) {
        //Utilize SQL to remove a challenge from challenges, should also remove references to it in completedchallenges
        
        //First verify that the challenge exists
        try {
            ResultSet r;
            stmt = c.createStatement();
            r = stmt.executeQuery("SELECT 1 FROM challenges WHERE challengename='" + challengeName + "'");
            if (r.next()) {
                //Exists
                //First delete references to the challenge within completed challenges
                stmt = c.createStatement();
                stmt.executeUpdate("DELETE FROM completedchallenges WHERE challengename='" + challengeName + "'");
                
                //And delete the actual challenge itself
                stmt = c.createStatement();
                stmt.executeUpdate("DELETE FROM challenges WHERE challengename='" + challengeName + "'"); //Remove the actual challenge
                
                c.commit();
                
                System.out.println("Challenge, " + challengeName + ", has been deleted");
            } else {
                System.out.println(challengeName + " cannot be removed, as it does not exist");
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    public static void createRank(String rankName, int xpRequired) {
        //Utilize SQL to create a new rank with the required XP
        try {
            //Verify that rank does not already exist
            ResultSet r;
            stmt = c.createStatement();
            r = stmt.executeQuery("SELECT 1 FROM ranks WHERE rankname='" + rankName + "'");
            
            if (r.next()) {
                //Already exists
                System.out.println(rankName + " already exists, remove it first, then re-add if you want to modify it");
                return;
            }
            
            //Add the rank
            stmt = c.createStatement();
            stmt.executeUpdate("INSERT INTO ranks (rankname,rankxp) VALUES ('" + rankName + "'," + Integer.toString(xpRequired) + ")");
            c.commit();
            System.out.println("Created new rank: " + rankName);
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    public static void removeRank(String rankName) {
        //Utilize SQL to remove a rank, and should also update the ranks for all players to ensure that it's correct for users
        
        //We'll need to first adjust all users that have this rank to have their appropriate rank in the absense of the one we're removing, but first...
        //Check if the rank even exists
        ArrayList<String> ranks;
        int rXP = -1;
        String prevRank = "";
        ResultSet r;
        try {
            stmt = c.createStatement();
            r = stmt.executeQuery("SELECT 1 FROM ranks WHERE rankname='" + rankName + "'");
            if (r.next()) {
                //Rank exists
                stmt = c.createStatement();
                r = stmt.executeQuery("SELECT rankxp FROM ranks WHERE rankname='" + rankName + "'");
                r.next();
                rXP = r.getInt(1);
                
                stmt = c.createStatement();
                r = stmt.executeQuery("SELECT * FROM ranks ORDER BY rankxp ASC");
                //Get the rank below this one
                while(r.next()) {
                    if (rXP > r.getInt("rankxp")) {
                        prevRank = r.getString("rankname");
                    }
                }
                
                //Now set all users with the tobe deleted rank to the previous rank
                stmt = c.createStatement();
                stmt.executeUpdate("UPDATE users SET rankname='" + prevRank + "' WHERE rankname='" + rankName + "'");
                
                //And delete the rank
                stmt = c.createStatement();
                stmt.executeUpdate("DELETE FROM ranks WHERE rankname='" + rankName + "'");
                System.out.println("Deleted rank, " + rankName + ", and set all users of that rank to " + prevRank);
                c.commit();
                
            } else {
                System.out.println("Could not remove rank, as no rank named " + rankName + " exists");
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    public static void removeGroup(String groupName) {
        //Utilize SQL to remove a group from groups, should also reflect changes to groupmembers
        try {
            ResultSet r;
            stmt = c.createStatement();
            r = stmt.executeQuery("SELECT 1 FROM groups WHERE groupname='" + groupName + "'");
            if (r.next()) {
                //Exists
                
                //First delete entries in groupmembers
                stmt = c.createStatement();
                stmt.executeUpdate("DELETE FROM groupMembers WHERE groupname='" + groupName + "'");
                
                //And delete the group's messages
                stmt = c.createStatement();
                stmt.executeUpdate("DELETE FROM messages WHERE groupname='" + groupName + "'");
                
                //And remove the actual group
                stmt = c.createStatement();
                stmt.executeUpdate("DELETE FROM groups WHERE groupname='" + groupName + "'");
                System.out.println("Deleted group, " + groupName);
                c.commit();
            } else {
                System.out.println("Cannot remove group named " + groupName + " as it does not exist");
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    public static void viewGroup(String groupName) {
        //Utilize SQL to view a group's basic info, and all members
        ArrayList<String> groupMembers = new ArrayList<String>();
        String groupLeader, groupDesc;
        
        try {
            ResultSet r;
            stmt = c.createStatement();
            r = stmt.executeQuery("SELECT 1 FROM groups WHERE groupname='" + groupName + "'");
            if (r.next()) {
                //Group exists
                
                stmt = c.createStatement();
                r = stmt.executeQuery("SELECT * FROM groups LEFT JOIN groupMembers ON groups.groupname=groupMembers.groupName WHERE groups.groupname='" + groupName + "'");
                
                r.next();
                groupLeader = r.getString("groupleader").replaceFirst("\\s++$", "");
                groupDesc = r.getString("groupdesc").replaceFirst("\\s++$", "");
                groupMembers.add(r.getString("username").replaceFirst("\\s++$", ""));
                while(r.next()) {
                    groupMembers.add(r.getString("username").replaceFirst("\\s++$", ""));
                }
                
                System.out.println("\nName: " + groupName);
                System.out.println("Leader: " + groupLeader);
                System.out.println("Description: " + groupDesc);
                System.out.println("Users:");
                for(int i = 0; i < groupMembers.size(); i++) {
                    System.out.println("\t" + groupMembers.get(i));
                }
                System.out.println();
            } else {
                System.out.println("No group by the name of " + groupName + " exists");
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    public static void viewGroupMessages(String groupName) {
        //Utilize SQL to view all messages on a group's board
        
        try {
            ResultSet r;
            stmt = c.createStatement();
            r = stmt.executeQuery("SELECT 1 FROM groups WHERE groupname='" + groupName + "'");
            if (r.next()) {
                //Exists
                
                //Fetch all the messages
                stmt = c.createStatement();
                r = stmt.executeQuery("SELECT creator, message FROM messages WHERE groupname='" + groupName + "'");
                
                System.out.println(groupName + "'s message board");
                int i = 1;
                while(r.next()) {
                    //Print messages
                    System.out.println("\t" + Integer.toString(i).replaceFirst("\\s++$", "") + ": " + r.getString("creator").replaceFirst("\\s++$", "") + " posted '" + r.getString("message").replaceFirst("\\s++$", "") + "'");
                    i++;
                }
                System.out.println();
            } else {
                System.out.println("No group by the name, " + groupName + " exists");
            }            
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }
    
    public static boolean attemptLogin(String username) {
        //Attempt to login as an admin
        try {
            //Execute SQL Queries to fetch the user (if they exist) and their type
            stmt = c.createStatement();
            ResultSet r = stmt.executeQuery("SELECT type FROM users WHERE username='" + username + "'");
            if (!r.next()) {
                //User does not exist
                System.out.println(username + " does not exist!");
                return false;
            }
            String result = r.getString(1);
            
            if (result.equals("admin")) {
                return true;
            } else {
                return false;
            }

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
        return false;
    }
    
    public static void userMenu(String input) {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String usr, rname, email, type;
        if (!input.equals("0")) {
            try {
            switch(input) {
                case "1":
                    //add user
                    System.out.print("\nFirst enter a username: ");
                    usr = br.readLine();
                    System.out.print("Real name: ");
                    rname = br.readLine();
                    System.out.print("Email address: ");
                    email = br.readLine();
                    System.out.print("And the type of user they are (user, admin): ");
                    type = br.readLine();
                    
                    //Attempt to add the user
                    createUser(usr,rname,email,type);
                    break;
                    
                case "2":
                    //remove user
                    System.out.print("\nEnter the username of who you would like to remove: ");
                    usr = br.readLine();
                    
                    //Attempt to remove the user
                    removeUser(usr);
                    break;
            }
            } catch (Exception e) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
            }
        }
    }
    
    public static void rankMenu(String input) {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String rankname, rankrequirement;
        if (!input.equals("0")) {
            try {
            switch(input) {
                case "1":
                    //add rank
                    System.out.print("\nEnter the rank name: ");
                    rankname = br.readLine();
                    System.out.print("Enter the rank XP requirement: ");
                    rankrequirement = br.readLine();
                    
                    //Perform operation
                    createRank(rankname, Integer.parseInt(rankrequirement));
                    break;
                    
                case "2":
                    //remove rank
                    System.out.print("\nEnter the rank name: ");
                    rankname = br.readLine();
                    
                    //Perform operation
                    removeRank(rankname);
                    break;
            }
            } catch (Exception e) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
            }
        }        
    }
    
    public static void groupMenu(String input) {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String groupname;
        if (!input.equals("0")) {
            try {
            switch(input) {
                case "1":
                    //view group
                    System.out.print("\nEnter the group's name: ");
                    groupname = br.readLine();
                    
                    //Perform
                    viewGroup(groupname);
                    break;
                    
                case "2":
                    //view group messages
                    System.out.print("\nEnter the group's name: ");
                    groupname = br.readLine();
                    
                    //Perform
                    viewGroupMessages(groupname);                    
                    break;
                    
                case "3":
                    //remove group
                    System.out.print("\nEnter the group's name: ");
                    groupname = br.readLine();
                    
                    //Perform
                    removeGroup(groupname);
                    break;
            }
            } catch (Exception e) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
            }
        }  
    }
    
    public static void challengeMenu(String input) {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String challengename, challengexp, challengedesc;
        if (!input.equals("0")) {
            try {
            switch(input) {
                case "1":
                    //Add challenge
                    System.out.print("\nEnter a challenge name: ");
                    challengename = br.readLine();
                    System.out.print("Enter the XP required to achieve: ");
                    challengexp = br.readLine();
                    System.out.print("Enter the description of the challenge: ");
                    challengedesc = br.readLine();
                    
                    //Perform
                    createChallenge(challengename, Integer.parseInt(challengexp), challengedesc);
                    break;
                    
                case "2":
                    //Remove challenge
                    System.out.print("\nEnter a challenge name: ");
                    challengename = br.readLine();
                    
                    //Perform
                    removeChallenge(challengename);
                    break;
            }
            } catch (Exception e) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
            }
        }          
    }
    
    public static void printMainMenu() {
        System.out.println("1 - User Administration");
        System.out.println("2 - Rank Administration");
        System.out.println("3 - Group Administration");
        System.out.println("4 - Challenge Administration");
        System.out.println("0 - Quit");
        System.out.print("\nAdminClient->");
    }
    
    public static void printUserMenu() {
        System.out.println("\nUser Administration");
        System.out.println("1 - Add new user");
        System.out.println("2 - Remove user");
        System.out.println("0 - Return");
        System.out.print("\nAdminClient->");
    }
    
    public static void printRankMenu() {
        System.out.println("\nRank Administration");
        System.out.println("1 - Add new rank");
        System.out.println("2 - Remove rank");
        System.out.println("0 - Return");
        System.out.print("\nAdminClient->");
    }
    
    public static void printGroupMenu() {
        System.out.println("\nGroup Administration");
        System.out.println("1 - View group information");
        System.out.println("2 - View group message board");
        System.out.println("3 - Remove group");
        System.out.println("0 - Return");
        System.out.print("\nAdminClient->");
    }

    public static void printChallengeMenu() {
        System.out.println("\nChallenge Administration");
        System.out.println("1 - Add new challenge");
        System.out.println("2 - Remove challenge");
        System.out.println("0 - Return");
        System.out.print("\nAdminClient->");
    }    

    static Connection c = null;
    static Statement stmt = null;
}
