/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gcloud.datastore;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class BaseDatastoreBatchWriterTest {

  private static final Key KEY1 = Key.builder("dataset1", "kind1", "name1").build();
  private static final Key KEY2 = Key.builder(KEY1, 1).build();
  private static final Key KEY3 = Key.builder(KEY1, 2).build();
  private static final IncompleteKey INCOMPLETE_KEY = IncompleteKey.builder(KEY1).build();
  private static final Entity ENTITY1 = Entity.builder(KEY1).build();
  private static final Entity ENTITY2 = Entity.builder(KEY2).set("bak", true).build();
  private static final Entity ENTITY3 = Entity.builder(KEY3).set("bak", true).build();
  private static final FullEntity<IncompleteKey> INCOMPLETE_ENTITY_1 =
      Entity.builder(INCOMPLETE_KEY).build();
  private static final FullEntity<IncompleteKey> INCOMPLETE_ENTITY_2 =
      Entity.builder(INCOMPLETE_KEY).set("name", "dan").build();

  private DatastoreBatchWriter batchWriter;

  private class DatastoreBatchWriter extends BaseDatastoreBatchWriter {

    private final Datastore datastore;

    protected DatastoreBatchWriter() {
      super("test");
      datastore = EasyMock.createMock(Datastore.class);
      IncompleteKey[] expected = {INCOMPLETE_KEY, INCOMPLETE_KEY};
      List<Key> result = ImmutableList.of(KEY2, KEY3);
      expect(datastore.allocateId(expected)).andReturn(result).times(0, 1);
      replay(datastore);
    }

    @Override
    protected Datastore datastore() {
      return datastore;
    }

    void finish() {
      verify(datastore);
    }
  }

  @Before
  public void setUp() {
    batchWriter = new DatastoreBatchWriter();
  }

  @After
  public void tearDown() {
    batchWriter.finish();
  }

  @Test
  public void testAdd() throws Exception {
    Entity entity2 =
        Entity.builder(ENTITY2).key(Key.builder(KEY1).name("name2").build()).build();
    com.google.datastore.v1beta3.Mutation pb1 = 
        com.google.datastore.v1beta3.Mutation.newBuilder().setInsert(ENTITY1.toPb()).build();
    com.google.datastore.v1beta3.Mutation pb2 = 
        com.google.datastore.v1beta3.Mutation.newBuilder().setInsert(entity2.toPb()).build();
    com.google.datastore.v1beta3.Mutation pb3 = 
        com.google.datastore.v1beta3.Mutation.newBuilder()
        .setInsert(Entity.builder(KEY2, INCOMPLETE_ENTITY_1).build().toPb()).build();
    com.google.datastore.v1beta3.Mutation pb4 = 
        com.google.datastore.v1beta3.Mutation.newBuilder()
        .setInsert(Entity.builder(KEY3, INCOMPLETE_ENTITY_2).build().toPb()).build();
    List<Entity> entities = batchWriter
        .add(ENTITY1, INCOMPLETE_ENTITY_1, INCOMPLETE_ENTITY_2, entity2);
    List<com.google.datastore.v1beta3.Mutation> mutationResultsPb = batchWriter.toMutationPb();
    assertEquals(pb1, mutationResultsPb.get(0).build());
    assertEquals(pb2, mutationResultsPb.get(1).build());
    assertEquals(pb3, mutationResultsPb.get(2).build());
    assertEquals(pb4, mutationResultsPb.get(3).build());
    assertEquals(ENTITY1, entities.get(0));
    assertEquals(Entity.builder(KEY2, INCOMPLETE_ENTITY_1).build(), entities.get(1));
    assertEquals(Entity.builder(KEY3, INCOMPLETE_ENTITY_2).build(), entities.get(2));
    assertEquals(entity2, entities.get(3));
  }

  @Test
  public void testAddAfterDelete() throws Exception {
    com.google.datastore.v1beta3.Mutation pb = com.google.datastore.v1beta3.Mutation.newBuilder()
        .setUpsert(ENTITY1.toPb())
        .build();
    batchWriter.delete(KEY1);
    batchWriter.add(ENTITY1);
    List<com.google.datastore.v1beta3.Mutation> mutationResultsPb = batchWriter.toMutationPb();
    assertEquals(pb, batchWriter.toMutationPb().build());
  }

  @Test(expected = DatastoreException.class)
  public void testAddDuplicate() throws Exception {
    batchWriter.add(ENTITY1);
    batchWriter.add(ENTITY1);
  }

  @Test(expected = DatastoreException.class)
  public void testAddAfterPut() throws Exception {
    batchWriter.put(ENTITY1);
    batchWriter.add(ENTITY1);
  }

  @Test(expected = DatastoreException.class)
  public void testAddAfterUpdate() throws Exception {
    batchWriter.update(ENTITY1);
    batchWriter.add(ENTITY1);
  }

  @Test(expected = DatastoreException.class)
  public void testAddWhenNotActive() throws Exception {
    batchWriter.deactivate();
    batchWriter.add(ENTITY1);
  }

  @Test
  public void testAddWithDeferredAllocation() throws Exception {
    com.google.datastore.v1beta3.Mutation pb = com.google.datastore.v1beta3.Mutation.newBuilder()
        .addInsert(ENTITY1.toPb())
        .addInsertAutoId(INCOMPLETE_ENTITY_1.toPb())
        .addInsertAutoId(INCOMPLETE_ENTITY_2.toPb())
        .build();
    batchWriter.addWithDeferredIdAllocation(ENTITY1, INCOMPLETE_ENTITY_1);
    batchWriter.addWithDeferredIdAllocation(INCOMPLETE_ENTITY_2);
    assertEquals(pb, batchWriter.toMutationPb().build());
  }

  @Test(expected = DatastoreException.class)
  public void testAddWithDeferredAllocationWhenNotActive() throws Exception {
    batchWriter.deactivate();
    batchWriter.addWithDeferredIdAllocation(INCOMPLETE_ENTITY_1);
  }

  @Test
  public void testUpdate() throws Exception {
    com.google.datastore.v1beta3.Mutation pb = com.google.datastore.v1beta3.Mutation.newBuilder()
        .addUpdate(ENTITY1.toPb())
        .addUpdate(ENTITY2.toPb())
        .addUpdate(ENTITY3.toPb())
        .build();
    batchWriter.update(ENTITY1, ENTITY2);
    batchWriter.update(ENTITY3);
    assertEquals(pb, batchWriter.toMutationPb().build());
  }

  @Test
  public void testUpdateAfterUpdate() throws Exception {
    Entity entity = Entity.builder(ENTITY1).set("foo", "bar").build();
    com.google.datastore.v1beta3.Mutation pb = com.google.datastore.v1beta3.Mutation.newBuilder()
        .addUpdate(entity.toPb())
        .build();
    batchWriter.update(ENTITY1);
    batchWriter.update(entity);
    assertEquals(pb, batchWriter.toMutationPb().build());
  }

  @Test
  public void testUpdateAfterAdd() throws Exception {
    Entity entity = Entity.builder(ENTITY1).set("foo", "bar").build();
    com.google.datastore.v1beta3.Mutation pb = com.google.datastore.v1beta3.Mutation.newBuilder()
        .addUpsert(entity.toPb())
        .build();
    batchWriter.add(ENTITY1);
    batchWriter.update(entity);
    assertEquals(pb, batchWriter.toMutationPb().build());
  }

  @Test
  public void testUpdateAfterPut() throws Exception {
    Entity entity = Entity.builder(ENTITY1).set("foo", "bar").build();
    com.google.datastore.v1beta3.Mutation pb = com.google.datastore.v1beta3.Mutation.newBuilder()
        .addUpsert(entity.toPb())
        .build();
    batchWriter.put(ENTITY1);
    batchWriter.update(entity);
    assertEquals(pb, batchWriter.toMutationPb().build());
  }

  @Test(expected = DatastoreException.class)
  public void testUpdateAfterDelete() throws Exception {
    batchWriter.delete(KEY1);
    batchWriter.update(ENTITY1, ENTITY2);
  }

  @Test(expected = DatastoreException.class)
  public void testUpdateWhenNotActive() throws Exception {
    batchWriter.deactivate();
    batchWriter.update(ENTITY1);
  }

  @Test
  public void testPut() throws Exception {
    com.google.datastore.v1beta3.Mutation pb = com.google.datastore.v1beta3.Mutation.newBuilder()
        .addUpsert(ENTITY1.toPb())
        .addUpsert(ENTITY2.toPb())
        .addUpsert(ENTITY3.toPb())
        .build();
    batchWriter.put(ENTITY1, ENTITY2);
    batchWriter.put(ENTITY3);
    assertEquals(pb, batchWriter.toMutationPb().build());
  }

  @Test
  public void testPutAfterPut() throws Exception {
    Entity entity = Entity.builder(ENTITY1).set("foo", "bar").build();
    com.google.datastore.v1beta3.Mutation pb = com.google.datastore.v1beta3.Mutation.newBuilder()
        .setUpsert(entity.toPb())
        .build();
    batchWriter.put(ENTITY1);
    batchWriter.put(entity);
    assertEquals(pb, batchWriter.toMutationPb().build());
  }

  @Test
  public void testPutAfterAdd() throws Exception {
    Entity entity = Entity.builder(ENTITY1).set("foo", "bar").build();
    com.google.datastore.v1beta3.Mutation pb = com.google.datastore.v1beta3.Mutation.newBuilder()
        .setUpsert(entity.toPb())
        .build();
    batchWriter.add(ENTITY1);
    batchWriter.put(entity);
    assertEquals(pb, batchWriter.toMutationPb().build());
  }

  @Test
  public void testPutAfterUpdate() throws Exception {
    Entity entity = Entity.builder(ENTITY1).set("foo", "bar").build();
    com.google.datastore.v1beta3.Mutation pb = com.google.datastore.v1beta3.Mutation.newBuilder()
        .setUpsert(entity.toPb())
        .build();
    batchWriter.update(ENTITY1);
    batchWriter.put(entity);
    assertEquals(pb, batchWriter.toMutationPb().build());
  }

  @Test
  public void testPutAfterDelete() throws Exception {
    Entity entity = Entity.builder(ENTITY1).set("foo", "bar").build();
    com.google.datastore.v1beta3.Mutation pb = com.google.datastore.v1beta3.Mutation.newBuilder()
        .setUpsert(entity.toPb())
        .build();
    batchWriter.delete(KEY1);
    batchWriter.put(entity);
    assertEquals(pb, batchWriter.toMutationPb().build());
  }

  @Test(expected = DatastoreException.class)
  public void testPutWhenNotActive() throws Exception {
    batchWriter.deactivate();
    batchWriter.put(ENTITY1);
  }

  @Test
  public void testDelete() throws Exception {
    com.google.datastore.v1beta3.Mutation pb = com.google.datastore.v1beta3.Mutation.newBuilder()
        .addDelete(KEY1.toPb())
        .addDelete(KEY2.toPb())
        .addDelete(KEY3.toPb())
        .build();
    batchWriter.delete(KEY1, KEY2);
    batchWriter.delete(KEY3);
    assertEquals(pb, batchWriter.toMutationPb().build());
  }

  @Test
  public void testDeleteAfterAdd() throws Exception {
    com.google.datastore.v1beta3.Mutation pb = com.google.datastore.v1beta3.Mutation.newBuilder()
        .addInsertAutoId(INCOMPLETE_ENTITY_1.toPb())
        .addDelete(KEY1.toPb())
        .build();
    batchWriter.add(ENTITY1);
    batchWriter.addWithDeferredIdAllocation(INCOMPLETE_ENTITY_1);
    batchWriter.delete(KEY1);
    assertEquals(pb, batchWriter.toMutationPb().build());
  }

  @Test
  public void testDeleteAfterUpdate() throws Exception {
    com.google.datastore.v1beta3.Mutation pb = com.google.datastore.v1beta3.Mutation.newBuilder()
        .addDelete(KEY1.toPb())
        .build();
    batchWriter.update(ENTITY1);
    batchWriter.delete(KEY1);
    assertEquals(pb, batchWriter.toMutationPb().build());
  }

  @Test
  public void testDeleteAfterPut() throws Exception {
    com.google.datastore.v1beta3.Mutation pb = com.google.datastore.v1beta3.Mutation.newBuilder()
        .setDelete(KEY1.toPb())
        .build();
    batchWriter.put(ENTITY1);
    batchWriter.delete(KEY1);
    assertEquals(pb, batchWriter.toMutationPb().build());
  }

  @Test(expected = DatastoreException.class)
  public void testDeleteWhenNotActive() throws Exception {
    batchWriter.deactivate();
    batchWriter.delete(KEY1);
  }
}

