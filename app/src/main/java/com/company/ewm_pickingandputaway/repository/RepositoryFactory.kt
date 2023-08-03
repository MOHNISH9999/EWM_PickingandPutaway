package com.company.ewm_pickingandputaway.repository

import com.sap.cloud.android.odata.crvwm_physicalinventory_srv_entities.CRVWM_PHYSICALINVENTORY_SRV_EntitiesMetadata.EntitySets
import com.sap.cloud.android.odata.crvwm_physicalinventory_srv_entities.PhysicalInventoryCount
import com.sap.cloud.android.odata.crvwm_physicalinventory_srv_entities.RepackHuItem

import com.sap.cloud.mobile.odata.EntitySet
import com.sap.cloud.mobile.odata.EntityValue
import com.sap.cloud.mobile.odata.Property
import com.company.ewm_pickingandputaway.service.OfflineWorkerUtil

import java.util.WeakHashMap

/*
 * Repository factory to construct repository for an entity set
 */
class RepositoryFactory
/**
 * Construct a RepositoryFactory instance. There should only be one repository factory and used
 * throughout the life of the application to avoid caching entities multiple times.
 */
{
    private val repositories: WeakHashMap<String, Repository<out EntityValue>> = WeakHashMap()

    /**
     * Construct or return an existing repository for the specified entity set
     * @param entitySet - entity set for which the repository is to be returned
     * @param orderByProperty - if specified, collection will be sorted ascending with this property
     * @return a repository for the entity set
     */
    fun getRepository(entitySet: EntitySet, orderByProperty: Property?): Repository<out EntityValue> {
        val cRVWM_PHYSICALINVENTORY_SRV_Entities = OfflineWorkerUtil.cRVWM_PHYSICALINVENTORY_SRV_Entities
        val key = entitySet.localName
        var repository: Repository<out EntityValue>? = repositories[key]
        if (repository == null) {
            repository = when (key) {
                EntitySets.physicalInventoryCountSet.localName -> Repository<PhysicalInventoryCount>(cRVWM_PHYSICALINVENTORY_SRV_Entities, EntitySets.physicalInventoryCountSet, orderByProperty)
                EntitySets.repackHuItemSet.localName -> Repository<RepackHuItem>(cRVWM_PHYSICALINVENTORY_SRV_Entities, EntitySets.repackHuItemSet, orderByProperty)
                else -> throw AssertionError("Fatal error, entity set[$key] missing in generated code")
            }
            repositories[key] = repository
        }
        return repository
    }

    /**
     * Get rid of all cached repositories
     */
    fun reset() {
        repositories.clear()
    }
}
