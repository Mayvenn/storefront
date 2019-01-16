(ns adventure.find-your-stylist
  (:require
   #?@(:cljs [[om.core :as om]
              [storefront.platform.messages :as messages]
              [storefront.platform.component-utils :as utils]
              [sablono.core :as sablono]])
   [adventure.components.basic-prompt :as basic-prompt]
   [adventure.components.header :as header]
   [adventure.handlers :as handlers]
   [adventure.keypaths :as keypaths]
   [storefront.keypaths :as storefront.keypaths]
   [storefront.accessors.experiments :as experiments]
   [storefront.component :as component]
   [storefront.events :as events]
   [storefront.components.ui :as ui]))



(defn ^:private query [data]
  (let [adventure-choices (get-in data keypaths/adventure-choices)
        hair-flow?        (-> adventure-choices :flow #{"match-stylist"})]
    {:background-image      "https://ucarecdn.com/54f294be-7d57-49ba-87ce-c73394231f3c/aladdinMatchingOverlayImagePurpleGR203Lm3x.png"
     :stylist-match-zipcode (get-in data keypaths/adventure-stylist-match-zipcode)
     :places-loaded? (get-in data storefront.keypaths/loaded-places)
     :header-data           {:current-step 6
                             :title        [:div.medium "Find Your Stylist"]
                             :subtitle     (str "Step " (if hair-flow? 2 3) " of 3")
                             :back-link    events/navigate-adventure-what-next}}))

(defn special-group [{:keys [label keypath value errors data-test class] :as input-attributes :or {class "col-12"}}
                     {:keys [ui-element args content]}]
  (let [error (first errors)]
    [:div.mb2.stacking-context
     [:div.flex.justify-center
      (ui/plain-text-field label keypath value (not (nil? error))
                        (-> input-attributes
                            (dissoc :label :keypath :value :errors)
                            (update :wrapper-class str " not-rounded x-group-item")))
      (ui-element (update args :class str " not-rounded x-group-item") content)]
     (ui/field-error-message error data-test)]))

#?(:cljs
   (defn ^:private places-component [{:keys [id address-keypath keypath value errors]} owner]
     (reify
       om/IDidMount
       (did-mount [this]
         (messages/handle-message events/adventure-zipcode-component-mounted {:address-elem    id
                                                                              :address-keypath address-keypath}))
       om/IRender
       (render [_]
         (sablono/html
          [:div
           (special-group {:keypath       keypaths/adventure-stylist-match-zipcode
                           :id id
                           :wrapper-class "pl3 bg-white border-none flex-auto"
                           :data-test     "stylist-match-zip"
                           :focused       true
                           :placeholder   "zipcode"
                           :value         value
                           :errors        nil
                           :data-ref      "stylist-match-zip"}
                          {:ui-element ui/teal-button
                           :content    "→"
                           :args       {:style          {:width  "45px"
                                                         :height "45px"}
                                        :disabled?      true
                                        :disabled-class "bg-light-gray gray"
                                        :class          " flex items-center justify-center medium"}})])))))

(defn component
  [{:keys [header-data places-loaded? background-image stylist-match-zipcode]} owner _]
  (component/create
   [:div.bg-lavender.white.center.flex.flex-auto.flex-column
    (when header-data
      (header/built-component header-data nil))
    [:div.flex.flex-column.items-center.justify-center
     {:style {:height              "246px"
              :background-image    (str "url(" background-image ")")
              :background-position "bottom"
              :background-repeat   "no-repeat"
              :background-size     "cover"}}
     [:div.pt8
      [:div.h3.medium.mb2.col-8.mx-auto "Where do you want to get your hair done?"]

      [:div.col-10.mx-auto
       #?(:cljs
          (when places-loaded?
            (om/build places-component {:id "stylist-match-zipcode"
                                        :value stylist-match-zipcode})))]]]]))

(defn built-component [data opts]
  (component/build component (query data) opts))
