/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.mongodb.persistence.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.conversions.Bson;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.ModelQueryImpl;
import org.flowable.engine.impl.persistence.entity.ModelEntity;
import org.flowable.engine.impl.persistence.entity.data.ModelDataManager;
import org.flowable.engine.repository.Model;
import org.flowable.mongodb.cfg.MongoDbProcessEngineConfiguration;
import org.flowable.mongodb.persistence.entity.MongoDbModelEntityImpl;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

/**
 * MongoDB implementation of the ModelDataManager.
 *
 * @author Joram Barrez
 */
public class MongoDbModelDataManager extends AbstractMongoDbDataManager<ModelEntity> implements ModelDataManager {

    public static final String COLLECTION_MODELS = "models";

    public MongoDbModelDataManager(MongoDbProcessEngineConfiguration processEngineConfiguration) {
        super(processEngineConfiguration);
    }

    @Override
    public String getCollection() {
        return COLLECTION_MODELS;
    }

    @Override
    public BasicDBObject createUpdateObject(Entity entity) {
        MongoDbModelEntityImpl model = (MongoDbModelEntityImpl) entity;
        BasicDBObject update = new BasicDBObject();

        if (model.getName() != null) {
            update.put("name", model.getName());
        }
        if (model.getKey() != null) {
            update.put("key", model.getKey());
        }
        if (model.getCategory() != null) {
            update.put("category", model.getCategory());
        }
        if (model.getCreateTime() != null) {
            update.put("createTime", model.getCreateTime());
        }
        if (model.getLastUpdateTime() != null) {
            update.put("lastUpdateTime", model.getLastUpdateTime());
        }
        if (model.getVersion() != null) {
            update.put("version", model.getVersion());
        }
        if (model.getMetaInfo() != null) {
            update.put("metaInfo", model.getMetaInfo());
        }
        if (model.getDeploymentId() != null) {
            update.put("deploymentId", model.getDeploymentId());
        }
        if (model.getEditorSourceValueId() != null) {
            update.put("editorSourceValueId", model.getEditorSourceValueId());
        }
        if (model.getEditorSourceExtraValueId() != null) {
            update.put("editorSourceExtraValueId", model.getEditorSourceExtraValueId());
        }
        if (model.getTenantId() != null) {
            update.put("tenantId", model.getTenantId());
        }
        update.put("latest", model.isLatest());

        return update;
    }

    @Override
    public ModelEntity create() {
        return new MongoDbModelEntityImpl();
    }

    @Override
    public void insert(ModelEntity modelEntity) {
        MongoDbModelEntityImpl latestModel = (MongoDbModelEntityImpl) findLatestModel(modelEntity.getKey());
        if (latestModel == null) {
            ((MongoDbModelEntityImpl) modelEntity).setLatest(true);
        } else if (modelEntity.getVersion() > latestModel.getVersion()) {
            latestModel.setLatest(false);
            ((MongoDbModelEntityImpl) modelEntity).setLatest(true);
        }

        super.insert(modelEntity);
    }

    protected ModelEntity findLatestModel(String key) {
        return getMongoDbSession().findOne(COLLECTION_MODELS, Filters.eq("key", key), Sorts.descending("version"), 1);
    }

    @Override
    public List<Model> findModelsByQueryCriteria(ModelQueryImpl query) {
        return getMongoDbSession().find(COLLECTION_MODELS, queryToFilter(query));
    }

    @Override
    public long findModelCountByQueryCriteria(ModelQueryImpl query) {
        return getMongoDbSession().count(COLLECTION_MODELS, queryToFilter(query));
    }

    protected Bson queryToFilter(ModelQueryImpl query) {
        List<Bson> filters = new ArrayList<>();

        if (query.getId() != null) {
            filters.add(Filters.eq("_id", query.getId()));
        }
        if (query.getCategory() != null) {
            filters.add(Filters.eq("category", query.getCategory()));
        }
        if (query.getCategoryLike() != null) {
            filters.add(Filters.regex("category", query.getCategoryLike().replace("%", ".*")));
        }
        if (query.getCategoryNotEquals() != null) {
            filters.add(Filters.ne("category", query.getCategoryNotEquals()));
        }
        if (query.getName() != null) {
            filters.add(Filters.eq("name", query.getName()));
        }
        if (query.getNameLike() != null) {
            filters.add(Filters.regex("name", query.getNameLike().replace("%", ".*")));
        }
        if (query.getKey() != null) {
            filters.add(Filters.eq("key", query.getKey()));
        }
        if (query.getVersion() != null) {
            filters.add(Filters.eq("version", query.getVersion()));
        }
        if (query.getDeploymentId() != null) {
            filters.add(Filters.eq("deploymentId", query.getDeploymentId()));
        }
        if (query.getTenantId() != null) {
            filters.add(Filters.eq("tenantId", query.getTenantId()));
        }
        if (query.getTenantIdLike() != null) {
            filters.add(Filters.regex("tenantId", query.getTenantIdLike().replace("%", ".*")));
        }
        if (query.isDeployed()) {
            filters.add(Filters.exists("deploymentId", true));
        }
        if (query.isNotDeployed()) {
            filters.add(Filters.exists("deploymentId", false));
        }
        if (query.isWithoutTenantId()) {
            filters.add(Filters.or(
                    Filters.eq("tenantId", ProcessEngineConfiguration.NO_TENANT_ID),
                    Filters.not(Filters.exists("tenantId"))
            ));
        }
        if (query.isLatest()) {
            filters.add(Filters.eq("latest", true));
        }

        return makeAndFilter(filters);
    }

    @Override
    public List<Model> findModelsByNativeQuery(Map<String, Object> parameterMap) {
        Bson filter = mapToFilter(parameterMap);
        return getMongoDbSession().find(COLLECTION_MODELS, filter);
    }

    @Override
    public long findModelCountByNativeQuery(Map<String, Object> parameterMap) {
        Bson filter = mapToFilter(parameterMap);
        return getMongoDbSession().count(COLLECTION_MODELS, filter);
    }

    protected Bson mapToFilter(Map<String, Object> parameterMap) {
        List<Bson> filters = new ArrayList<>();
        for (Map.Entry<String, Object> entry : parameterMap.entrySet()) {
            filters.add(Filters.eq(entry.getKey(), entry.getValue()));
        }
        return makeAndFilter(filters);
    }

}
