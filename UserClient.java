import java.sql.*;
import java.io.*;
import java.util.Random;

/**
  * @author Original: Andrew Sudduth
 */

public class UserClient{

   static Connection conn;
   static Statement stmt;
   
   public static void main(String[] args) throws IOException, NumberFormatException{
      BufferedReader reader = new BufferedReader(new InputStreamReader (System.in));
      try{
      //SQL Connection
      String db_username="oracle_username", password = "oracle_password";
      DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
      conn = DriverManager.getConnection (
                   "oracle_url",
		             db_username, password);
      conn.setAutoCommit(true);
      
      //Handle Login
      String username;
      System.out.println("Please enter your username [examples are testUser1 and testUser2]: ");
      username = reader.readLine();
      while(userLogin(username)==false){
          username = reader.readLine();
      }
      int run = 1;
      int option = 0;
      
      //Give options for the main menu
      while(run==1){
         System.out.print("What would you like to do?\n0 - Quit\n1 - Play Game\n2 - View All Challenges\n" +
          "3 - View Your Completed Challenges\n4 - View Your Stats\n5 - View Top Players\n6 - Visit Group System\n>> ");
         option = Integer.parseInt(reader.readLine());
         
         switch(option){
            case 1:
               playGame(username);
               break;
            case 2:
               seeAllChallenges();
               break;
            case 3:
               seeCompletedChallenges(username);
               break;
            case 4:
               viewStats(username);
               break;
            case 5:
               seeTopPlayers();
               break;
            case 6:
               groupSystem(username);
               break;
            case 0:
               run=0;
               break;
           default:
               break;
         }
         
      }
      conn.close(); //Close DB Connection
      
      }catch(SQLException e){
         System.out.println("Caught SQL Exception: \n     " + e);
      }
   }//End Main
   
   //Handle user login
   public static boolean userLogin(String u){
      try{
         stmt = conn.createStatement();
         ResultSet login = stmt.executeQuery("SELECT username FROM users WHERE username='"+u+"'");
         login.next();
         //Check to ensure login actually exists
         if(login.getString(1)!=null){
            return true;
         }
      }catch(SQLException e){
         System.out.println("Incorrect username...please try again: ");
      }
      return false;
   }//End User Login
   
   public static void playGame(String u){
      //"Play Game"
      
      //Generate random number
      Random rand = new Random();
      int  xp = rand.nextInt(4);
      
      //User looses game
      if(xp==0){
         System.out.println("You attempted to fight the good fight, but were defeated.\nNo experience Gained.");
      }else{
      
         //User wins the game
         int oldXP,newXP;
         String currentRank;
         try{
            //Get users informaton from the database and place it into variables
            stmt = conn.createStatement();
            ResultSet userInfo = stmt.executeQuery("SELECT xp, rankname FROM users WHERE username='"+u+"'");
            userInfo.next();
            
            oldXP = Integer.parseInt(userInfo.getString(1));
            currentRank = userInfo.getString(2).replaceFirst("\\s++$", "");
            newXP = oldXP+xp;
            
            //Inform user of victory
            System.out.println("You fought like a manly man.\n You gained "+xp+" XP");
            
            //Update XP in the database
            stmt = conn.createStatement();
            stmt.executeUpdate("UPDATE users SET xp='"+newXP+"' WHERE username='"+u+"'");
            stmt = conn.createStatement();
            
            //Check to see the rank the user should be 
            ResultSet rankNeeded = stmt.executeQuery("SELECT rankname FROM ranks WHERE rankxp < "+newXP+" ORDER BY rankxp DESC");
            rankNeeded.next();
            String neededRank = rankNeeded.getString(1).replaceFirst("\\s++$", "");
            
            //Rank the user up if required
            if(!(currentRank.equals(neededRank))){
               stmt = conn.createStatement();
               stmt.executeUpdate("UPDATE users SET rankname='"+neededRank+"' WHERE username='"+u+"'");
               System.out.println("You ranked up to: "+neededRank);
            }
            
            //Check for completed Challenges
            stmt = conn.createStatement();
            ResultSet challenges = stmt.executeQuery("SELECT challengename FROM challenges WHERE xpreq < '"+newXP+"'");
            while(challenges.next()){
               //Check to see if challenge has been completed
               stmt = conn.createStatement();
               ResultSet c = stmt.executeQuery("SELECT 1 FROM completedchallenges WHERE challengename='"+challenges.getString(1)+"' AND username = '"+u+"'");
               
               //Detemine if there was a result from the above query
               if(!(c.next())){
                  
                  //Mark the challenge as complete
                  stmt = conn.createStatement();
                  stmt.executeUpdate("INSERT INTO completedchallenges VALUES ('"+u+"','"+challenges.getString(1)+"')");
                  System.out.println("You have completed the challenge: " + challenges.getString(1).replaceFirst("\\s++$", ""));
               }
            }
            //SELECT 1 FROM challengesCompleted WHERE challengename= [challenge] AND username = [user]
         }catch(SQLException e){
            System.out.println("Unable to update database: " + e);
         }
      }
   }//End PlayGame Method
   
   //Display statistics about current player
   public static void viewStats(String u){
      try{
         //Query and display information from database
         stmt = conn.createStatement();
         ResultSet userInfo = stmt.executeQuery("SELECT xp, rankname FROM users WHERE username='"+u+"'");
         userInfo.next();
         
         System.out.println("Username: "+u+"\nXP: "+userInfo.getString(1)+"\tRank: "+userInfo.getString(2).replaceFirst("\\s++$", ""));
      }catch(SQLException e){
         System.out.println("There was an error getting your stats");
      }
   }
  
   //Display the top players (XP wise) in the system
   public static void seeTopPlayers(){
      try{
         //Query database for top 10 players
         stmt = conn.createStatement();
         ResultSet players = stmt.executeQuery("SELECT username, name, rankname, xp FROM users WHERE ROWNUM <= 10 ORDER BY xp DESC");
         
         //Print out table of top users
         System.out.println("User\tName\tRank\tXP");
         while(players.next()){
            System.out.println(players.getString(1).replaceFirst("\\s++$", "")
            +"\t"+players.getString(2).replaceFirst("\\s++$", "")+"\t"+players.getString(3).replaceFirst("\\s++$", "")
            +"\t"+players.getString(4).replaceFirst("\\s++$", ""));
         }
      }catch(SQLException e){
          System.out.println("SQL Done Goofed: " + e);
      }
   }
   
   //See all challenges that are available to complete
   public static void seeAllChallenges(){
      try{
         //Query the database for the challenges
         stmt = conn.createStatement();
         ResultSet allChallenges = stmt.executeQuery("SELECT * FROM challenges");
         
         //Set up table with information about the challeneges available
         System.out.println("Challenge Name\tRequired XP\tDescription");
         while(allChallenges.next()){
            System.out.println(allChallenges.getString(1).replaceFirst("\\s++$", "")
            +"\t"+allChallenges.getString(2)
            +"\t\t"+allChallenges.getString(3).replaceFirst("\\s++$", ""));
         }
      }catch(SQLException e){
         System.out.println("Unable to view all challenges: "+ e);
      }
   }
   
   //See the challenges completed by a user, given a username
   public static void seeCompletedChallenges(String u){
      try{
         //Query the database for the information
         stmt = conn.createStatement();
         ResultSet allChallenges = stmt.executeQuery("SELECT * FROM challenges c, completedchallenges cc WHERE c.challengename=cc.challengename AND cc.username='"+u+"'");
         
         //Display the table with the given infromation
         System.out.println("Challenge Name\tRequired XP\tDescription");
         while(allChallenges.next()){
            System.out.println(allChallenges.getString(1).replaceFirst("\\s++$", "")
            +"\t"+allChallenges.getString(2)
            +"\t\t"+allChallenges.getString(3).replaceFirst("\\s++$", ""));
         }
      }catch(SQLException e){
         System.out.println("Unable to view all challenges: " + e);
      }
   }
   
   //This method controls the menu for the group system
   public static void groupSystem(String u) throws IOException{
   
      //Prepare information to be read in
      BufferedReader reader = new BufferedReader(new InputStreamReader (System.in));
      int input;
      String group;
      int runGroup = 1;
      
      //Actual menu itself and controlling code
      System.out.println("Welcome to the Group System!");
      while(runGroup==1){
         System.out.print("What would you to do?\n0 - Return to main menu\n1 - Create Group\n2 - View Groups Your In\n3 - Visit Specific Group\n>>");
         input = Integer.parseInt(reader.readLine());
         switch(input){
            case 0:
               runGroup=0;   
               break;
            case 1:
               createGroup(u);
               break;
            case 2:
               viewGroups(u);
               break;
            case 3:
               System.out.print("Which group would you like to visit?\n>>");
               group = reader.readLine();
               visitGroup(group, u);
         }
      }
   }//End Group System
   
   //Method to create group given the owner's username
   public static void createGroup(String u) throws IOException{
   
      //Prepare to read in information
      BufferedReader reader = new BufferedReader(new InputStreamReader (System.in));
      
      //Get requested group name
      System.out.print("What would you like your group to be called?\n >>");
      String gName = reader.readLine();
      try{
      
         //Check for the existance of a group with the requested name
         stmt = conn.createStatement();
         ResultSet groupExist = stmt.executeQuery("SELECT groupname FROM groups WHERE groupname='"+gName+"'");
         if(groupExist.next()){
            System.out.println("That group already exists, please try again...");
            createGroup(u);
         }
         
         //Request description for the group
         System.out.print("Please give a short description of your group\n>>");
         String gDesc = reader.readLine();
         
         //Create the group, and add leader into member list for the group
         stmt.executeUpdate("INSERT INTO groups VALUES ('"+gName+"','"+gDesc+"','"+u+"')");
         stmt.executeUpdate("INSERT INTO groupMembers VALUES ('"+u+"','"+gName+"')");
         
         
         System.out.println("That new group was created!");
      }catch(SQLException e){
         System.out.println("There was an error inserting your group to the database: " + e);
      }
   }
   //Same method from AdminClient, just modified to fit here
   public static void removeGroup(String groupName) {
        //Utilize SQL to remove a group from groups, should also reflect changes to groupmembers
        try {
            ResultSet r;
            stmt = conn.createStatement();
            r = stmt.executeQuery("SELECT 1 FROM groups WHERE groupname='" + groupName + "'");
            if (r.next()) {
                //Exists
                
                //First delete entries in groupmembers
                stmt = conn.createStatement();
                stmt.executeUpdate("DELETE FROM groupMembers WHERE groupname='" + groupName + "'");
                
                //And delete the group's messages
                stmt = conn.createStatement();
                stmt.executeUpdate("DELETE FROM messages WHERE groupname='" + groupName + "'");
                
                //And remove the actual group
                stmt = conn.createStatement();
                stmt.executeUpdate("DELETE FROM groups WHERE groupname='" + groupName + "'");
                System.out.println("Deleted group, " + groupName);
            } else {
                System.out.println("Cannot remove group named " + groupName + " as it does not exist");
            }
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }
   
   //View the groups which a given user is appart of
   public static void viewGroups(String u){
      try{
         //Search the database for a match
         stmt = conn.createStatement();
         ResultSet groups = stmt.executeQuery("SELECT g.groupName, g.groupDesc, g.groupLeader FROM groups g, groupmembers m WHERE g.groupname=m.groupname AND m.username='"+u+"'");
         
         //Prepare table to hold the data
         System.out.println("Group Name\tGroup Leader\t Description");
         while(groups.next()){
            System.out.println(groups.getString(1).replaceFirst("\\s++$", "")
            +"\t"+groups.getString(3).replaceFirst("\\s++$", "")
            +"\t"+groups.getString(2).replaceFirst("\\s++$", ""));
         }
      }catch(SQLException e){
         System.out.println("SQL done goofed...: " + e);
      }
   }
   
   //Provide interface for a specific group
   public static void visitGroup(String g, String u) throws IOException{
   BufferedReader reader = new BufferedReader(new InputStreamReader (System.in));
      try{
         //Create to make sure user is given correct priviledges
         boolean isMember,isLeader;
         
         //Query the database for information on the group
         stmt = conn.createStatement();
         ResultSet membership = stmt.executeQuery("SELECT * FROM groupmembers WHERE username='"+u+"' AND groupname='"+g+"'");
         isMember = membership.next();
         ResultSet groups = stmt.executeQuery("SELECT * FROM groups WHERE groupname='"+g+"'");
         
         //Ensure the group exists
         if(groups.next()==false){
            System.out.println("That group does not exist!");
            groupSystem(u);
         }
         
         //Display and control menu for the group being viewed
         System.out.println("Group Information:\nName: "+groups.getString(1).replaceFirst("\\s++$", "")+"\nLeader: "+groups.getString(3).replaceFirst("\\s++$", "")+"\nDescription: "+groups.getString(2).replaceFirst("\\s++$", ""));
         isLeader = groups.getString(3).replaceFirst("\\s++$", "").equals(u);
         int menu = 1;
         int input;
         while(menu==1){
            //If you're a member of the group
            if(isMember==true){
               System.out.println("Member Options\n0 - Return\n1 - View Messages\n2 - Create Message\n3 - View Members");
            } else {
               System.out.println("You're a guest to this group.\nPress 0 to go back.");
            }
            
            //If you're the leader of the group
            if(isLeader==true){
               System.out.print("\nLeader Options: \n4 - Add user to group\n5 - Remove user from group\n6 - Remove this group");
            }
            System.out.print(">>");
            input=Integer.parseInt(reader.readLine());
            switch(input){
               case 0:
                  menu=0;
                  break;
               case 1:
                  viewMessages(g);
                  break;
               case 2:
                  if(isMember==true){
                     createMessage(g,u);
                  }
                  break;
               case 3:
                  if(isMember==true){
                     viewGroupUsers(g);
                  }
                  break;
               case 4:
                  if(isLeader==true){
                     addUserToGroup(g);
                  }                  break;
               case 5:
                  if(isLeader==true){
                     removeUserFromGroup(g);
                  }
                  break;
               case 6:
                  if(isLeader==true){
                     removeGroup(g);
                     groupSystem(u);
                  }
                  break;
            }
         }
         
      }catch(SQLException e){
         System.out.println("There was an error visiting this group: " + e);
      }
   }
   
   //Add a user to a given group
   public static void addUserToGroup(String g) throws IOException{
      //Prepare to read information in
      BufferedReader reader = new BufferedReader(new InputStreamReader (System.in));
      try{
         //Who would you like to add?
         stmt = conn.createStatement();
         System.out.print("Who would you like to add to this group?\n >>");
         String uName = reader.readLine();
         
         //Check for existance of the user
         ResultSet userExists = stmt.executeQuery("SELECT username FROM users WHERE username='"+uName+"'");
         if(userExists.next()){
         
            //If user exists, then add the user to the group
            stmt.executeUpdate("INSERT INTO groupmembers VALUES ('"+uName+"','"+g+"')");
            System.out.print("User has been added!");
            
         }else{
         
            //User does not exist, please try again!
            System.out.println("That user does not exist...please try again!");
            addUserToGroup(g);
         }
      }catch(SQLException e){
         System.out.println("That user is already in the group!");
      }
   }//end addUserToGroup()
   
   //Remove user from a specific group
   public static void removeUserFromGroup(String g) throws IOException{
      BufferedReader reader = new BufferedReader(new InputStreamReader (System.in));
      try{
         
         //Ask group leader who they would like to remove
         stmt = conn.createStatement();
         System.out.print("Who would you like to remove from this group?\n>>");
         String user = reader.readLine();
         
         //Attempt ot delete the user
         stmt.executeUpdate("DELETE FROM groupmembers WHERE groupname='"+g+"' AND username='"+user+"'");
         System.out.println("That user was deleted from the group!");
      }catch(SQLException e){
         System.out.println("There was an error removing that user..perhaps it doesnt exist?");
      }
   }
   
   //See the users who belong to a group given a the group name
   public static void viewGroupUsers(String g){
      try{
      
         //Query the database for the users of the group
         stmt = conn.createStatement();
         ResultSet members = stmt.executeQuery("SELECT g.username, u.name FROM groupmembers g, users u WHERE u.username=g.username AND g.groupname='"+g+"'");
         
         //Place the information into a somewhat nice table
         System.out.println("Username\tReal Name");
         while(members.next()){
            System.out.println(members.getString(1).replaceFirst("\\s++$", "")
            +"\t"+members.getString(2).replaceFirst("\\s++$", ""));
         }
      }catch(SQLException e){
         System.out.println("There was an error viewing the the users: " + e);
      }
   }//End View Group Users
   
   //Create a message within the given group
   public static void createMessage(String g, String u) throws IOException{
   
      //Get the message from the user to add
      BufferedReader reader = new BufferedReader(new InputStreamReader (System.in));
      System.out.println("What would you like to say?\n>>");
      String msg = reader.readLine();
      
      try{
      
         //Insert the message into the database
         stmt.executeUpdate("INSERT INTO messages (message,groupname,creator) VALUES ('"+msg+"','"+g+"','"+u+"')");
         System.out.println("Your message was added!");
      }catch(SQLException e){
         System.out.println("There was an error adding that message: " + e);
      }
   }//End Create Messages
   
   //View the last 100 messages within a given grpi[
   public static void viewMessages(String g){
      try{
      
         //Retrieve the message information from the database
         stmt = conn.createStatement();
         System.out.println("Messages for this group (last 100): ");
         ResultSet messages = stmt.executeQuery("SELECT message, creator FROM messages WHERE groupname='"+g+"' AND ROWNUM <=100 ORDER BY messageid DESC");
         
         //Display the messages placed into the group
         while(messages.next()){
            System.out.println(messages.getString(2).replaceFirst("\\s++$", "")+" posted '"+messages.getString(1).replaceFirst("\\s++$", "")+"'");
         }
      }catch(SQLException e){
         System.out.println("There was an error retrieving the messages: " + e);
      }  
   }//End viewMessages
}  //End Class
