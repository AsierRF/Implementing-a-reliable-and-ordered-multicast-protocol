import mcgui.Message;

/**
 * @author Asier Rivera Fernandez
 * @version 1.0
 */
public class OrderMessage extends Message {
	
	/**Saves the value of the order in which the message with the related unique ID must be delivered.*/
	int orderNumber;
	/**Saves the value of the unique ID that this order number belongs to, links this order with the proper message.*/
	String messageUniqueID;
	
	/**
	 * Constructs a new object of the class {@link OrderMessage}.
	 * @param sender ID of the sender node.
	 * @param orderNumber The Order Number of the message.
	 * @param messageUniqueID The Unique ID of the message.
	 */
	public OrderMessage(int sender, int orderNumber, String messageUniqueID) {
		super(sender);
		this.orderNumber=orderNumber;
		this.messageUniqueID=messageUniqueID;
	}
	/**
	 * Returns the Order Number of the message.
	 * @return The Order Number of the message.
	 */
	public int getOrderNumber() {
		return orderNumber;
	}
	/**
	 * Returns the Unique ID of the message.
	 * @return The Unique ID of the message.
	 */
	public String getMessageUniqueID() {
		return messageUniqueID;
	}

	private static final long serialVersionUID = 1L;
	
}
