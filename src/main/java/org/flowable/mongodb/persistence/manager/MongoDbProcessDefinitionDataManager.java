/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.mongodb.persistence.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.conversions.Bson;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.ProcessDefinitionQueryImpl;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.data.ProcessDefinitionDataManager;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.mongodb.cfg.MongoDbProcessEngineConfiguration;
import org.flowable.mongodb.persistence.entity.MongoDbProcessDefinitionEntityImpl;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Filters;

public class MongoDbProcessDefinitionDataManager extends AbstractMongoDbDataManager<ProcessDefinitionEntity>
        implements ProcessDefinitionDataManager {

    public static final String COLLECTION_PROCESS_DEFINITIONS = "processDefinitions";

    public MongoDbProcessDefinitionDataManager(MongoDbProcessEngineConfiguration processEngineConfiguration) {
        super(processEngineConfiguration);
    }

    @Override
    public String getCollection() {
        return COLLECTION_PROCESS_DEFINITIONS;
    }

    @Override
    public ProcessDefinitionEntity create() {
        return new MongoDbProcessDefinitionEntityImpl();
    }

    @Override
    public BasicDBObject createUpdateObject(Entity entity) {
        MongoDbProcessDefinitionEntityImpl processDefinitionEntity = (MongoDbProcessDefinitionEntityImpl) entity;
        BasicDBObject updateObject = null;
        updateObject = setUpdateProperty(processDefinitionEntity, "latest", processDefinitionEntity.isLatest(), updateObject);
        updateObject = setUpdateProperty(processDefinitionEntity, "category", processDefinitionEntity.getCategory(), updateObject);
        updateObject = setUpdateProperty(processDefinitionEntity, "suspensionState", processDefinitionEntity.getSuspensionState(), updateObject);
        return updateObject;
    }

    @Override
    public void insert(ProcessDefinitionEntity entity) {
        MongoDbProcessDefinitionEntityImpl latest = (MongoDbProcessDefinitionEntityImpl)
                findLatestProcessDefinitionByKeyAndTenantId(entity.getKey(), entity.getTenantId());

        if (latest == null || entity.getVersion() > latest.getVersion()) {
            if (latest != null) {
                latest.setLatest(false);
                getMongoDbSession().update(entity);
            }
            ((MongoDbProcessDefinitionEntityImpl) entity).setLatest(true);
        }

        super.insert(entity);
    }

    @Override
    public ProcessDefinitionEntity findLatestProcessDefinitionByKey(String key) {
        return getMongoDbSession().findOne(COLLECTION_PROCESS_DEFINITIONS,
                Filters.and(Filters.eq("key", key), Filters.eq("latest", true)));
    }

    @Override
    public ProcessDefinitionEntity findLatestProcessDefinitionByKeyAndTenantId(String key, String tenantId) {
        return getMongoDbSession().findOne(COLLECTION_PROCESS_DEFINITIONS,
                Filters.and(Filters.eq("key", key), Filters.eq("latest", true), Filters.eq("tenantId", tenantId)));
    }

    @Override
    public ProcessDefinitionEntity findLatestDerivedProcessDefinitionByKey(String key) {
        return getMongoDbSession().findOne(COLLECTION_PROCESS_DEFINITIONS,
                Filters.and(Filters.eq("key", key), Filters.eq("derivedFrom", true), Filters.eq("latest", true)));
    }

    @Override
    public ProcessDefinitionEntity findLatestDerivedProcessDefinitionByKeyAndTenantId(String key, String tenantId) {
        return getMongoDbSession().findOne(COLLECTION_PROCESS_DEFINITIONS,
                Filters.and(Filters.eq("key", key), Filters.eq("derivedFrom", true), Filters.eq("latest", true), Filters.eq("tenantId", tenantId)));
    }

    @Override
    public void deleteProcessDefinitionsByDeploymentId(String deploymentId) {
        getMongoDbSession().bulkDelete(COLLECTION_PROCESS_DEFINITIONS, Filters.eq("deploymentId", deploymentId));
    }

    @Override
    public List<ProcessDefinition> findProcessDefinitionsByQueryCriteria(ProcessDefinitionQueryImpl query) {
        return getMongoDbSession().find(COLLECTION_PROCESS_DEFINITIONS, createFilter(query));
    }

    @Override
    public long findProcessDefinitionCountByQueryCriteria(ProcessDefinitionQueryImpl query) {
        return getMongoDbSession().count(COLLECTION_PROCESS_DEFINITIONS, createFilter(query));
    }

    @Override
    public ProcessDefinitionEntity findProcessDefinitionByDeploymentAndKey(String deploymentId, String key) {
        return getMongoDbSession().findOne(COLLECTION_PROCESS_DEFINITIONS,
                Filters.and(Filters.eq("deploymentId", deploymentId), Filters.eq("key", key),
                        Filters.or(Filters.eq("tenantId", ProcessEngineConfiguration.NO_TENANT_ID),
                                Filters.not(Filters.exists("tenantId")))));
    }

    @Override
    public ProcessDefinitionEntity findProcessDefinitionByDeploymentAndKeyAndTenantId(String deploymentId, String key, String tenantId) {
        return getMongoDbSession().findOne(COLLECTION_PROCESS_DEFINITIONS,
                Filters.and(Filters.eq("deploymentId", deploymentId), Filters.eq("key", key), Filters.eq("tenantId", tenantId)));
    }

    @Override
    public ProcessDefinitionEntity findProcessDefinitionByParentDeploymentAndKey(String parentDeploymentId, String key) {
        return getMongoDbSession().findOne(COLLECTION_PROCESS_DEFINITIONS,
                Filters.and(Filters.eq("parentDeploymentId", parentDeploymentId), Filters.eq("key", key)));
    }

    @Override
    public ProcessDefinitionEntity findProcessDefinitionByParentDeploymentAndKeyAndTenantId(String parentDeploymentId, String key, String tenantId) {
        return getMongoDbSession().findOne(COLLECTION_PROCESS_DEFINITIONS,
                Filters.and(Filters.eq("parentDeploymentId", parentDeploymentId), Filters.eq("key", key), Filters.eq("tenantId", tenantId)));
    }

    @Override
    public ProcessDefinitionEntity findProcessDefinitionByKeyAndVersion(String key, Integer version) {
        return getMongoDbSession().findOne(COLLECTION_PROCESS_DEFINITIONS,
                Filters.and(Filters.eq("key", key), Filters.eq("version", version),
                        Filters.or(Filters.eq("tenantId", ProcessEngineConfiguration.NO_TENANT_ID),
                                Filters.not(Filters.exists("tenantId")))));
    }

    @Override
    public ProcessDefinitionEntity findProcessDefinitionByKeyAndVersionAndTenantId(String key, Integer version, String tenantId) {
        return getMongoDbSession().findOne(COLLECTION_PROCESS_DEFINITIONS,
                Filters.and(Filters.eq("key", key), Filters.eq("version", version), Filters.eq("tenantId", tenantId)));
    }

    @Override
    public List<ProcessDefinition> findProcessDefinitionsByNativeQuery(Map<String, Object> parameterMap) {
        Bson filter = createBsonFromParameters(parameterMap);
        return getMongoDbSession().find(COLLECTION_PROCESS_DEFINITIONS, filter);
    }

    @Override
    public long findProcessDefinitionCountByNativeQuery(Map<String, Object> parameterMap) {
        Bson filter = createBsonFromParameters(parameterMap);
        return getMongoDbSession().count(COLLECTION_PROCESS_DEFINITIONS, filter);
    }

    @Override
    public void updateProcessDefinitionTenantIdForDeployment(String deploymentId, String newTenantId) {
        Bson filter = Filters.eq("deploymentId", deploymentId);
        BasicDBObject update = new BasicDBObject("$set", new BasicDBObject("tenantId", newTenantId));
        getMongoDbSession().bulkUpdate(COLLECTION_PROCESS_DEFINITIONS, filter, update);
    }

    @Override
    public void updateProcessDefinitionVersionForProcessDefinitionId(String processDefinitionId, int version) {
        Bson filter = Filters.eq("_id", processDefinitionId);
        BasicDBObject update =  new BasicDBObject("version", version);
        getMongoDbSession().updateOne(COLLECTION_PROCESS_DEFINITIONS, filter, update);
    }

    protected Bson createFilter(ProcessDefinitionQueryImpl query) {
        List<Bson> filters = new ArrayList<>();
        if (query.getId() != null) filters.add(Filters.eq("_id", query.getId()));
        if (query.getIds() != null) filters.add(Filters.in("_id", query.getIds()));
        if (query.getCategory() != null) filters.add(Filters.eq("category", query.getCategory()));
        if (query.getCategoryLike() != null) filters.add(Filters.regex("category", query.getCategoryLike()));
        if (query.getCategoryNotEquals() != null) filters.add(Filters.ne("category", query.getCategoryNotEquals()));
        if (query.getName() != null) filters.add(Filters.eq("name", query.getName()));
        if (query.getNameLike() != null) filters.add(Filters.regex("name", query.getNameLike()));
        if (query.getDeploymentId() != null) filters.add(Filters.eq("deploymentId", query.getDeploymentId()));
        if (query.getDeploymentIds() != null) filters.add(Filters.in("deploymentId", query.getDeploymentIds()));
        if (query.getKey() != null) filters.add(Filters.eq("key", query.getKey()));
        if (query.getKeyLike() != null) filters.add(Filters.regex("key", query.getKeyLike()));
        if (query.getResourceName() != null) filters.add(Filters.eq("resourceName", query.getResourceName()));
        if (query.getResourceNameLike() != null) filters.add(Filters.regex("resourceName", query.getResourceNameLike()));
        if (query.getVersion() != null) filters.add(Filters.eq("version", query.getVersion()));
        if (query.getVersionGt() != null) filters.add(Filters.gt("version", query.getVersionGt()));
        if (query.getVersionGte() != null) filters.add(Filters.gte("version", query.getVersionGte()));
        if (query.getVersionLt() != null) filters.add(Filters.lt("version", query.getVersionLt()));
        if (query.getVersionLte() != null) filters.add(Filters.lte("version", query.getVersionLte()));
        if (query.isLatest()) filters.add(Filters.eq("latest", true));
        if (query.getSuspensionState() != null) filters.add(Filters.eq("suspensionState", query.getSuspensionState().getStateCode()));
        if (query.getEngineVersion() != null) filters.add(Filters.eq("engineVersion", query.getEngineVersion()));
        if (query.getTenantId() != null) filters.add(Filters.eq("tenantId", query.getTenantId()));
        if (query.getTenantIdLike() != null) filters.add(Filters.regex("tenantId", query.getTenantIdLike().replace("%", ".*")));
        if (query.isWithoutTenantId()) {
            filters.add(Filters.or(Filters.eq("tenantId", ProcessEngineConfiguration.NO_TENANT_ID), Filters.not(Filters.exists("tenantId"))));
        }
        return makeAndFilter(filters);
    }

    protected Bson createBsonFromParameters(Map<String, Object> parameterMap) {
        List<Bson> filters = new ArrayList<>();
        for (Map.Entry<String, Object> entry : parameterMap.entrySet()) {
            filters.add(Filters.eq(entry.getKey(), entry.getValue()));
        }
        return makeAndFilter(filters);
    }
}
