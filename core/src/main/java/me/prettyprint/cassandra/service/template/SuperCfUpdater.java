package me.prettyprint.cassandra.service.template;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.prettyprint.cassandra.model.HSuperColumnImpl;
import me.prettyprint.cassandra.serializers.BooleanSerializer;
import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.DateSerializer;
import me.prettyprint.cassandra.serializers.DoubleSerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.TypeInferringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.ColumnFactory;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;

/**
 * This provides an interface of updating a specified row, most likely with the
 * contents of an object. This would likely by implemented as an anonymous inner
 * class with access to a final object in scope. It would update the given row
 * with the object's data.
 * 
 * This is currently implemented as an abstract base class instead of an
 * interface. This could change in the future. Being an abstract base class
 * allows CassandraTemplate to initialize this instance through package scope
 * field access. This means that implementation of update() simply makes
 * consecutive calls to various set****() methods which already have the
 * contextual information they need to update the correct row.
 * 
 * The downside of this approach is that the updater is essentially stateful and
 * cannot be used concurrently. The alternative is to pass an object in to
 * update() as a parameter with the setter methods, leaving the updater to be
 * stateless.
 * 
 * @author david
 * @since Mar 10, 2011
 * @param <K>
 *          the key's data type
 * @param <SN>
 *          the standard or super column's data type
 * @param <N>
 *          the child column name type in a super column
 * @param <V>
 *          the object instance to persist
 */
public class SuperCfUpdater<K,SN,N> extends AbstractTemplateUpdater<K, N> {
  private static final Logger log = LoggerFactory.getLogger(SuperCfUpdater.class);
  protected SuperCfTemplate<K,SN, N> template;
  private List<SN> sColumnNames;
  private int sColPos;
  private HSuperColumnImpl<SN,N,?> activeColumn;
  private List<HColumn> subColumns;
  
  public SuperCfUpdater(SuperCfTemplate<K,SN,N> sTemplate, ColumnFactory columnFactory) {
    super((AbstractColumnFamilyTemplate<K, N>) sTemplate, columnFactory);
    this.template = sTemplate;
  }
  
  
  
  @Override
  public SuperCfUpdater<K, SN, N> addKey(K key) {        
    
    if ( keys != null && keys.size() > 0 ) {
      updateInternal();
    }
    super.addKey(key);
    sColumnNames = new ArrayList<SN>();
    sColPos = 0;
    return this;
  }



  public SuperCfUpdater<K,SN,N> addSuperColumn(SN sColumnName) {
    if ( sColumnNames.size() > 0 ) {
      updateInternal();
      sColPos++;      
    }
    
    subColumns = new ArrayList<HColumn>();
    
    sColumnNames.add(sColumnName);
    
    
    return this;
  }
  
  public SN getCurrentSuperColumn() {
    return sColumnNames.get(sColPos);
  }
  
  /**
   * collapse the state of the active HSuperColumn 
   */
  void updateInternal() {    
    // HSuperColumnImpl needs a refactor, this construction is lame.
    // the value serializer is not used in HSuperColumnImpl, so this is safe for name
    if ( !subColumns.isEmpty() ) {
      log.debug("Adding column {} for key {} and cols {}", new Object[]{getCurrentSuperColumn(), getCurrentKey(), subColumns});
      HSuperColumnImpl<SN, N, ?> column = new HSuperColumnImpl(getCurrentSuperColumn(), subColumns, 
          0, template.getTopSerializer(), template.getSubSerializer(), TypeInferringSerializer.get());
      template.getMutator().addInsertion(getCurrentKey(), template.getColumnFamily(), column);
    }

  }

  /**
   * Deletes the super column and all of its sub columns
   */
  public void deleteSuperColumn() {
    //template.getMutator().addDeletion(getCurrentKey(), template.getColumnFamily(), 
    //    getCurrentSuperColumn(), template.getTopSerializer());    
    template.getMutator().addSuperDelete(getCurrentKey(), template.getColumnFamily(), 
        getCurrentSuperColumn(), template.getTopSerializer());  
  }
  
  public void deleteSubColumn(N columnName) {
    template.getMutator().addSubDelete(getCurrentKey(), template.getColumnFamily(), 
        getCurrentSuperColumn(), columnName, template.getTopSerializer(), template.getSubSerializer());
  }

  public void setString(N subColumnName, String value) {
    subColumns.add(columnFactory.createColumn(subColumnName, value, template.getEffectiveClock(),
        template.getSubSerializer(), StringSerializer.get() ));
  }

  public void setUUID(N subColumnName, UUID value) {
    subColumns.add(columnFactory.createColumn(subColumnName, value, template.getEffectiveClock(),
        template.getSubSerializer(), UUIDSerializer.get()));
  }

  public void setLong(N subColumnName, Long value) {
    subColumns.add(columnFactory.createColumn(subColumnName, value, template.getEffectiveClock(),
        template.getSubSerializer(), LongSerializer.get()));
  }

  public void setInteger(N subColumnName, Integer value) {
    subColumns.add(columnFactory.createColumn(subColumnName, value, template.getEffectiveClock(),
        template.getSubSerializer(), IntegerSerializer.get()));
  }

  public void setBoolean(N subColumnName, Boolean value) {
    subColumns.add(columnFactory.createColumn(subColumnName, value, template.getEffectiveClock(),
        template.getSubSerializer(), BooleanSerializer.get())); 
  }

  public void setByteArray(N subColumnName, byte[] value) {
    subColumns.add(columnFactory.createColumn(subColumnName, value, template.getEffectiveClock(),
        template.getSubSerializer(), BytesArraySerializer.get()));
  }

  public void setByteBuffer(N subColumnName, ByteBuffer value) {
    subColumns.add(columnFactory.createColumn(subColumnName, value, template.getEffectiveClock(),
        template.getSubSerializer(), ByteBufferSerializer.get()));
  }
  
  public void setDate(N subColumnName, Date value) {
    subColumns.add(columnFactory.createColumn(subColumnName, value, template.getEffectiveClock(),
        template.getSubSerializer(), DateSerializer.get()));
  }
  
  public void setDouble(N subColumnName, Double value) {
    subColumns.add(columnFactory.createColumn(subColumnName, value, template.getEffectiveClock(),
        template.getSubSerializer(), DoubleSerializer.get()));
  }
  
  public <V> void setValue(N subColumnName, V value, Serializer<V> serializer) {
    subColumns.add(columnFactory.createColumn(subColumnName, value, template.getEffectiveClock(),
        template.getSubSerializer(), serializer));
  }
}
