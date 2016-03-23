import mcgui.*;

/**
 * @author Asier Rivera Fernandez
 * @version 1.0
 */
public class TextMessage extends Message {
        
	/**Saves the text of the message.*/
    String text;
    /**Saves the unique ID of the message.*/
	String uniqueID;
	
	/**
	 * Constructs a new object of the class {@link TextMessage}.
	 * @param sender ID of the sender node.
	 * @param text The text of the message.
	 * @param uniqueID The Unique ID of the message.
	 */
	public TextMessage(int sender, String text, String uniqueID)	{
		super(sender);
		this.text=text;
		this.uniqueID=uniqueID;
	}    
    /**
     * Returns the text of the message.
     * @return The text of the message.
     */
    public String getText() {
        return text;
    }
    /**
     * Returns the Unique ID of the message.
     * @return The Unique ID of the message.
     */
	public String getUniqueID()	{
		return this.uniqueID;
	}
	
    public static final long serialVersionUID = 0;
}
