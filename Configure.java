import akka.actor.ActorRef;

public final class Configure {
	
	private String file;
	private ActorRef collectionActor;

	public Configure(String file, ActorRef collectionActor) {
		this.file = file;
		this.collectionActor = collectionActor;
	}
	
	public String getFile() {
		return this.file;
	}
	
	public ActorRef getCollectionActor() {
		return this.collectionActor;
	}
}
