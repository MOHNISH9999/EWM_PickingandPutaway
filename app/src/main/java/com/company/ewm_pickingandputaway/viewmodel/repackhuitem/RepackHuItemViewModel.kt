package com.company.ewm_pickingandputaway.viewmodel.repackhuitem

import android.app.Application
import android.os.Parcelable

import com.company.ewm_pickingandputaway.viewmodel.EntityViewModel
import com.sap.cloud.android.odata.crvwm_physicalinventory_srv_entities.RepackHuItem
import com.sap.cloud.android.odata.crvwm_physicalinventory_srv_entities.CRVWM_PHYSICALINVENTORY_SRV_EntitiesMetadata.EntitySets

/*
 * Represents View model for RepackHuItem
 *
 * Having an entity view model for each <T> allows the ViewModelProvider to cache and return the view model of that
 * type. This is because the ViewModelStore of ViewModelProvider cannot not be able to tell the difference between
 * EntityViewModel<type1> and EntityViewModel<type2>.
 */
class RepackHuItemViewModel(application: Application): EntityViewModel<RepackHuItem>(application, EntitySets.repackHuItemSet, RepackHuItem.warehouseNumber) {
    /**
     * Constructor for a specific view model with navigation data.
     * @param [navigationPropertyName] - name of the navigation property
     * @param [entityData] - parent entity (starting point of the navigation)
     */
    constructor(application: Application, navigationPropertyName: String, entityData: Parcelable): this(application) {
        EntityViewModel<RepackHuItem>(application, EntitySets.repackHuItemSet, RepackHuItem.warehouseNumber, navigationPropertyName, entityData)
    }
}
