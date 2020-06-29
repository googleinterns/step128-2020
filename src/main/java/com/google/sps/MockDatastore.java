import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Index;
import com.google.appengine.api.datastore.Index.IndexState;
import com.google.appengine.api.datastore.DatastoreAttributes;
import java.util.Map;
import java.util.HashMap;

public class MockDatastore implements DatastoreService {

  
  @Override
  public Map<Index,Index.IndexState> getIndexes() {
     return new HashMap<>();
  }

  @Override 
  public DatastoreAttributes getDatastoreAttributes() {
    return new DatastoreAttributes("");
  }

//   DatastoreService.KeyRangeState	allocateIdRange(KeyRange range)
// This method allocates a user-specified contiguous range of unique IDs.
// KeyRange	allocateIds(Key parent,java.lang.String kind,long num)
// IDs are allocated within a namespace defined by a parent key and a kind.
// KeyRange	allocateIds(java.lang.String kind,long num)
// IDs are allocated within a namespace defined by a parent key and a kind.
// Transaction	beginTransaction()
// Equivalent to beginTransaction(TransactionOptions.Builder.withDefaults()).
// Transaction	beginTransaction(TransactionOptions options)
// Begins a transaction against the datastore.
// void	delete(java.lang.Iterable<Key> keys)
// Equivalent to delete(Key...).
// void	delete(Key... keys)
// Deletes the Entity entities specified by keys.
// void	delete(Transaction txn,java.lang.Iterable<Key> keys)
// Exhibits the same behavior as delete(Iterable), but executes within the provided transaction.
// void	delete(Transaction txn,Key... keys)
// Exhibits the same behavior as delete(Key...), but executes within the provided transaction.
// java.util.Map<Key,Entity>	get(java.lang.Iterable<Key> keys)
// Retrieves the set of Entities matching keys.
// Entity	get(Key key)
// Retrieves the Entity with the specified Key.
// java.util.Map<Key,Entity>	get(Transaction txn,java.lang.Iterable<Key> keys)
// Exhibits the same behavior as get(Iterable), but executes within the provided transaction.
// Entity	get(Transaction txn,Key key)
// Exhibits the same behavior as get(Key), but executes within the provided transaction.
// DatastoreAttributes	getDatastoreAttributes()
// Retrieves the current datastore's attributes.
// java.util.Map<Index,Index.IndexState>	getIndexes()
// Returns the application indexes and their states.
// Key	put(Entity entity)
// If the specified Entity does not yet exist in the data store, create it and assign its Key.
// java.util.List<Key>	put(java.lang.Iterable<Entity> entities)
// Performs a batch put of all entities.
// Key	put(Transaction txn,Entity entity)
// Exhibits the same behavior as put(Entity), but executes within the provided transaction.
// java.util.List<Key>	put(Transaction txn,java.lang.Iterable<Entity> entities)

  @Override 
  public static class DatastoreServiceFactory {
    
    public DatastoreService getDatastoreService() {
      return new MockDatastore();
    }
  }
}