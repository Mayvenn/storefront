(ns checkout.shop.edit-appointment-menu
  (:require [storefront.component :as c]
            [storefront.components.popup :as popup]
            [storefront.events :as e]
            [appointment-booking.core :as booking.core]
            mayvenn.concept.booking
            [mayvenn.concept.follow :as follow]
            [storefront.components.header :as components.header]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.transitions :as t]
            [storefront.platform.messages
             :refer [handle-message]
             :rename {handle-message publish}]
            [storefront.effects :as fx]))

(defmethod popup/component :edit-appointment-menu
  [data _ _]
  (c/html
   (ui/modal
    {:body-style  {:max-width "625px"}
     :close-attrs (utils/fake-href e/control-addon-service-menu-dismiss)
     :col-class   "col-12"}
    [:div.bg-white
     (components.header/mobile-nav-header
      {:class ""}
      nil
      nil
      (c/html [:a (merge {:data-test "edit-appointment-popup-close"}
                         (utils/fake-href e/control-dismiss-edit-appointment-menu))
               (svg/close-x {:class  "stroke-black fill-white"
                             :width  "2em"
                             :height "2em"})]))
     (c/build booking.core/modal-body data)])))

(defmethod popup/query :edit-appointment-menu
  [state]
  (booking.core/modal-query state))

(defmethod t/transition-state e/control-show-edit-appointment-menu
  [_ _ _ state]
  (assoc-in state keypaths/popup :edit-appointment-menu))

(defmethod fx/perform-effects e/control-show-edit-appointment-menu
  [_ _ _args _ _state]
  (publish e/biz|follow|defined
           {:follow/start    [e/biz|appointment-booking|initialized]
            :follow/after-id e/biz|appointment-booking|done
            :follow/then     [e/biz|appointment-booking|navigation-decided
                              {:choices {:success e/control-dismiss-edit-appointment-menu}}]}))

(defmethod t/transition-state e/control-dismiss-edit-appointment-menu
  [_ _ _ state]
  (assoc-in state keypaths/popup nil))

