import mcgui.*;
import java.util.ArrayList;
/**
 * @author Asier Rivera Fernandez
 * @version 1.0
 */
public class RTOCaster extends Multicaster {

	/**Value that indicates a crashed state.*/
	static int Crashed = 0;
	/**Value that indicates a running state.*/
	static int Running = 1;
	/**Saves whether the node is running or crashed.*/
	int stateNode [];
	/**Saves local unique character for the "casted" messages, joined with the 'uniqueIDnumber' creates unique IDs for messages.*/
	String uniqueIDchar;
	/**Saves local order number for the "casted" messages, joined with the 'uniqueIDchar' creates unique IDs for messages.*/
	int uniqueIDnumber;						
	/**Saves the ID number of the node that is the sequencer.*/
	int sequencerID;
	/**Saves the sequencer number, the sequencer uses it to set a order number to next message to be delivered.*/
	int sequencerNumber;
	/**Saves the local order number, in order to know next message to be delivered.*/
	int localNumber;
	/**Holds up buffer for received but not delivered messages.*/
	ArrayList <TextMessage> received;
	/**Holds up buffer for delivered messages.*/
	ArrayList <TextMessage> delivered;
	/**Hold up buffer for received OrderMessages.*/
	ArrayList <OrderMessage> orderMessages;
	
    /**
     * Initializes the variables needed for the execution.
     */
    public void init() {
        mcui.debug("The network has "+hosts+" hosts!");
		stateNode = new int [hosts];
		for (int i=0; i<hosts; i++)	{
			stateNode[i] = RTOCaster.Running;
		}
		uniqueIDchar = Character.toString((char) (65+id));
		uniqueIDnumber = 0;
		sequencerID = 0;
		sequencerNumber = 0;
		localNumber = 0;
		received = new ArrayList <TextMessage> ();
		delivered = new ArrayList <TextMessage> ();
		orderMessages = new ArrayList <OrderMessage> ();
		mcui.debug("The new Coordinator is: "+String.valueOf(sequencerID));
    }
        
    /**
     * The GUI calls this module to multicast a message.
     * @param messagetext The text of the message to be sent.
     */
    public void cast(String messagetext) {
        broadcastTextMessage(messagetext, new String(uniqueIDchar+String.valueOf(uniqueIDnumber)));
        this.uniqueIDnumber++;
        mcui.debug("Sent out: \""+messagetext+"\"");
    }
    
    /**
     * Receive a basic message and handle it properly: Deliver it, Rebroadcast it, Create the {@link OrderMessage}, Save it in the buffer, etc.
     * @param peer The ID of the peer that sent the message.
     * @param message The message received.
     */
    public void basicreceive(int peer, Message message) {
    	if(isNew(message))	{
    		if(id!=peer)	{
				broadcastMessage(message);
			}
    		String messageType = message.getClass().getName();
    		if (messageType.equals("TextMessage"))	{
    			TextMessage newTMessage = (TextMessage) message;
    			if(id==this.sequencerID)	{
    				broadcastOrderMessage(newTMessage.getUniqueID());
    		    }
    			if (canBeDelivered(newTMessage))	{
    				toDeliver(newTMessage);
    				checkQueue();
    			} else	{
    				this.received.add(new TextMessage(newTMessage.getSender(), newTMessage.getText(), newTMessage.getUniqueID()));
    			}
    		}else if (messageType.equals("OrderMessage"))	{
    			OrderMessage newOMessage = (OrderMessage) message;
    			if (canBeDelivered(newOMessage))	{
    				toDeliver(newOMessage);
    				checkQueue();
    			} else	{
    				this.orderMessages.add(new OrderMessage(newOMessage.getSender(), newOMessage.orderNumber, newOMessage.getMessageUniqueID()));
    			}
    		}
    	}
    }
    
	/**
     * Signals that a peer is down and has been down for a while to
     * allow for messages taking different paths from this peer to
     * arrive.
     * @param peer ID of the peer that crashed.
     */
    public void basicpeerdown(int peer) {
    	mcui.debug("Peer "+peer+" has been dead for a while now!");
		stateNode[peer]=RTOCaster.Crashed;
		if(peer==sequencerID)	{
			mcui.debug("The Coordinator is dead, looking for the new one.");
			for(int i=((peer+1)%hosts); i!=peer; i=((i+1)%hosts))	{
				if(stateNode[i]==RTOCaster.Running){
					sequencerID=i;
					break;
				}
			}
			mcui.debug("The new Coordinator is: "+String.valueOf(sequencerID));
		}
		if(id==sequencerID){
			checkQueue();
			this.sequencerNumber=localNumber;
			checkMessagesToOrder();
		}
    }
    /**
     * Broadcasts the message to all nodes that are {@link RTOCaster#Running}.
     * @param msg Message to be sent.
     */
    private void broadcastMessage(Message msg)	{
    	for(int i=0; i < hosts; i++) {
            if(stateNode[i]==RTOCaster.Running) {
                bcom.basicsend(i, msg);
            }
        }
    }
    /**
     * Broadcasts the message of class {@link TextMessage} to all nodes that are {@link RTOCaster#Running}.
     * @param textmessage Text to be sent.
     * @param uniqueID Unique ID for this message.
     */
    private void broadcastTextMessage(String textmessage, String uniqueID)	{
    	for(int i=0; i < hosts; i++) {
            if(stateNode[i]==RTOCaster.Running) {
                bcom.basicsend(i,new TextMessage(id, textmessage, uniqueID));
            }
        }
    }
    /**
     * Broadcasts the message of class {@link OrderMessage} to all nodes that are {@link RTOCaster#Running}.
     * @param uniqueID Unique ID for this message.
     */
    private void broadcastOrderMessage(String uniqueID)	{
    	for(int i=(id+1)%hosts, max=0; max<hosts; i=((i+1)%hosts)) {
            if(stateNode[i]==RTOCaster.Running) {
                bcom.basicsend(i,new OrderMessage(id, this.sequencerNumber, uniqueID));
            }
            max++;
        }
    	this.sequencerNumber++;
    }
    /**
     * Checks whether the given message is new and should be handled or it has already been handled and it should be dismissed.
     * @param message The message to be checked.
     * @return True if the message given is new or False if it is not.
     */
    private boolean isNew(Message message) {
    	String messageType = message.getClass().getName();
    	if (messageType.equals("TextMessage"))	{
    		TextMessage newMessage = (TextMessage) message;
    		for(int i=0;i<this.delivered.size();i++)	{
    			if (this.delivered.get(i).getUniqueID().equals(newMessage.getUniqueID()))	{
    				return false;
    			}
    		}
			for(int i=0;i<this.received.size();i++)	{
				if (this.received.get(i).getUniqueID().equals(newMessage.getUniqueID()))	{
					return false;
				}
			}
		}else if (messageType.equals("OrderMessage"))	{
			OrderMessage newMessage = (OrderMessage) message;
			if(this.localNumber>newMessage.getOrderNumber())	{
				return false;
			}
			for(int i=0;i<this.orderMessages.size();i++)	{
				if (this.orderMessages.get(i).getMessageUniqueID().equals(newMessage.getMessageUniqueID()))	{
					return false;
				}
			}
		}
		return true;
	}
    /**
     * Checks whether the given message can be delivered or not, looking if its {@link OrderMessage} has arrived and if it is its turn.
     * @param msg The message to be checked.
     * @return True if the given message can be delivered or False if it can't.
     */
    private boolean canBeDelivered(TextMessage msg)	{
    	for(int i=0; i<this.orderMessages.size(); i++)	{
    		if(msg.getUniqueID().equals(this.orderMessages.get(i).getMessageUniqueID())){
    			if (this.orderMessages.get(i).getOrderNumber()>this.localNumber)	{
    				return false;
    			} else {
    				return true;
    			}
    		}
    	}
    	return false;
    }
    /**
     * Checks whether the given message can be delivered or not, looking if its {@link TextMessage} has arrived and if it is its turn.
     * @param msg The message to be checked.
     * @return True if the given message can be delivered or False if it can't.
     */
    private boolean canBeDelivered(OrderMessage msg)	{
    	if (msg.getOrderNumber()>this.localNumber)	{
    		return false;
    	}
    	for(int i=0; i<this.received.size(); i++)	{
    		if(msg.getMessageUniqueID().equals(this.received.get(i).getUniqueID())){
    			return true;
    		}
    	}
    	return false;
    }
    /**
     * Delivers the given {@link TextMessage}.
     * @param msg The {@link TextMessage} to be delivered.
     */
    private void toDeliver(TextMessage msg)	{
    	for(int i=0; i<this.orderMessages.size();i++)	{
    		if(this.orderMessages.get(i).getMessageUniqueID().equals(msg.getUniqueID()))	{
    			this.orderMessages.remove(i);
    			break;
    		}
    	}    	
    	this.delivered.add(new TextMessage(msg.getSender(), msg.getText(), msg.getUniqueID()));
    	this.localNumber++;
    	mcui.deliver(msg.getSender(), msg.getText(), (msg.getSender()==id ? " from myself" :""));
    }
    /**
     * Delivers the {@link TextMessage} related to the given {@link OrderMessage}.
     * @param msg The {@link OrderMessage} related to the {@link TextMessage} to be delivered.
     */
    private void toDeliver(OrderMessage msg)	{
    	TextMessage found = null;
    	for(int i=0; i<this.received.size();i++)	{
    		if(this.received.get(i).getUniqueID().equals(msg.getMessageUniqueID()))	{
    			this.delivered.add(found=new TextMessage(this.received.get(i).getSender(), this.received.get(i).getText(), this.received.get(i).getUniqueID()));
    			this.received.remove(i);
    			break;
    		}
    	}
    	this.localNumber++;
    	if (found!=null) {
    		mcui.deliver(found.getSender(), found.getText(), (found.getSender()==id ? " from myself" :""));
    	}
    }
    /**
     * Checks whether there is more related {@link TextMessage} and {@link OrderMessage} that can be delivered.
     */
    private void checkQueue()	{
    	for(int i=0; i<this.received.size(); i++)	{
    		if(canBeDelivered(this.received.get(i)))	{
    			toDeliver(this.received.get(i));
    			this.received.remove(i);
    			i=0;
    		}
    	}
    }
    /**
     * Checks for those messages that arrived while the Sequencer was crashed so they will never have a order and they won't be delivered.
     */
    private void checkMessagesToOrder()	{
    	boolean hasOrder = false;
    	for(int i=0; i<this.received.size(); i++){
    		for(int e=0; e<this.orderMessages.size(); e++)	{
    			if(this.received.get(i).getUniqueID().equals(this.orderMessages.get(e).getMessageUniqueID()))	{
    				hasOrder=true;
    				e=this.orderMessages.size();
    			}
    		}
    		if(!hasOrder)	{
    			broadcastOrderMessage(this.received.get(i).getUniqueID());
    		}
    		hasOrder=false;
    	}
    }
}
