package com.company.ewm_pickingandputaway.viewmodel.warehousetaskexceptioncodetype

import android.app.Application
import android.os.Parcelable

import com.company.ewm_pickingandputaway.viewmodel.EntityViewModel
import com.sap.cloud.android.odata.cds_xcrvwmxapi_whse_order_task_entities.WarehouseTaskExceptionCodeType
import com.sap.cloud.android.odata.cds_xcrvwmxapi_whse_order_task_entities.cds_xcrvwmxapi_whse_order_task_EntitiesMetadata.EntitySets

/*
 * Represents View model for WarehouseTaskExceptionCodeType
 *
 * Having an entity view model for each <T> allows the ViewModelProvider to cache and return the view model of that
 * type. This is because the ViewModelStore of ViewModelProvider cannot not be able to tell the difference between
 * EntityViewModel<type1> and EntityViewModel<type2>.
 */
class WarehouseTaskExceptionCodeTypeViewModel(application: Application): EntityViewModel<WarehouseTaskExceptionCodeType>(application, EntitySets.warehouseTaskExceptionCode, WarehouseTaskExceptionCodeType.warehouse) {
    /**
     * Constructor for a specific view model with navigation data.
     * @param [navigationPropertyName] - name of the navigation property
     * @param [entityData] - parent entity (starting point of the navigation)
     */
    constructor(application: Application, navigationPropertyName: String, entityData: Parcelable): this(application) {
        EntityViewModel<WarehouseTaskExceptionCodeType>(application, EntitySets.warehouseTaskExceptionCode, WarehouseTaskExceptionCodeType.warehouse, navigationPropertyName, entityData)
    }
}
