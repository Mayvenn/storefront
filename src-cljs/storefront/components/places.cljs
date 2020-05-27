(ns storefront.components.places
  (:require [storefront.component :as c :refer [defdynamic-component]]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.components.ui :as ui]))

(defdynamic-component component
  (did-mount [this]
             (let [{:keys [address-keypath id]} (c/get-props this)]
               (handle-message events/checkout-address-component-mounted {:address-elem    id
                                                                          :address-keypath address-keypath})))
  (render [this]
          (let [{:keys [focused id keypath value data-test errors max-length]} (c/get-props this)]
            (c/html
             (ui/text-field {:data-test   data-test
                             :errors      errors
                             :id          id
                             :keypath     keypath
                             :focused     focused
                             :label       "Address"
                             :name        id
                             :on-key-down utils/suppress-return-key
                             :required    true
                             :type        "text"
                             :max-length  max-length
                             :value       value})))))
