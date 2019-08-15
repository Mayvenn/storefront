(ns adventure.stylist-matching.stylist-gallery
  (:require #?@(:cljs
                [[goog.dom]
                 [goog.events.EventType :as EventType]
                 [goog.events]
                 [goog.style]
                 [om.core :as om]
                 [storefront.api :as api]
                 [storefront.browser.scroll :as scroll]
                 [storefront.platform.messages :as messages]])
            [adventure.components.header :as header]
            [adventure.keypaths :as keypaths]
            [spice.core :as spice]
            [storefront.component :as component]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            storefront.keypaths
            [storefront.platform.component-utils :as utils]
            [storefront.transitions :as transitions]
            [stylist-directory.stylists :as stylists]))

(defn query
  [data]
  (let [stylist-id (get-in data keypaths/stylist-profile-id)
        stylist    (stylists/stylist-by-id data stylist-id)
        back       (first (get-in data storefront.keypaths/navigation-undo-stack))]
    (when stylist
      {:header-data {:subtitle     [:div.mt2.h4.medium
                                    (str (stylists/->display-name stylist) "'s Recent work" )]
                     :header-attrs {:class "bg-too-light-lavender black border-bottom border-gray border-width-2"}
                     :right-corner {:id    "close-stylist-gallery"
                                    :opts (utils/route-back-or-to back
                                                                  events/navigate-adventure-stylist-profile
                                                                  {:stylist-id stylist-id
                                                                   :store-slug (:store-slug stylist)})
                                    :value (svg/simple-x {:class "dark-gray"
                                                          :style {:width  "20px"
                                                                  :height "20px"}})}}
       :gallery     (map (comp ui/ucare-img-id :resizable-url) (:gallery-images stylist))})))

(defn sticky-organism
  [data owner {:component/keys [child ref mobile-overhead desktop-overhead] :as opts}]
  #?(:clj (component/create [:div])
     :cljs
     (letfn [(component-overhead-magic-number [] (if (< (.-width (goog.dom/getViewportSize)) 750) mobile-overhead desktop-overhead))
             (handle-scroll [e] (om/set-state! owner :show? (< (component-overhead-magic-number) (.-y (goog.dom/getDocumentScroll)))))
             (set-height [] (om/set-state! owner :component-height (some-> owner (om/get-node ref) goog.style/getSize .-height)))]
       (reify
         om/IInitState
         (init-state [this]
           {:show? false})
         om/IDidMount
         (did-mount [this]
           (set-height)
           (goog.events/listen js/window EventType/SCROLL handle-scroll))
         om/IWillUnmount
         (will-unmount [this]
           (goog.events/unlisten js/window EventType/SCROLL handle-scroll))
         om/IWillReceiveProps
         (will-receive-props [this next-props]
           (set-height))
         om/IRenderState
         (render-state [this {:keys [show? component-height]}]
           (component/html
            [:div.fixed.z4.top-0.left-0.right-0
             (if show?
               {:style {:margin-top "0px"}
                :class "transition-2"}
               {:class "hide"
                :style {:margin-top (str "-" component-height "px")}})
             [:div.col-12 {:ref ref}
              (om/build child data nil)]]))))))

(defn component
  [{:keys [header-data gallery]} owner opts]
  (component/create
   [:div.col-12.bg-white.mb6
    (when header-data
      [:div
       (header/built-component header-data nil)])
    [:div {:style {:height "72px"}}]
    (when header-data
      [:div
       (component/build sticky-organism
                        (-> header-data
                            (update-in [:header-attrs :class] str " mx-auto max-580")
                            (assoc :unstick? true))
                        {:opts {:component/mobile-overhead  70
                                :component/desktop-overhead 70
                                :component/ref              "header"
                                :component/child            header/component}})])
    (map-indexed (fn [ix image-id]
                   (ui/ucare-img {:class    "col-12"
                                  :width    580
                                  :data-ref (str "offset-" ix)} image-id))
                 gallery)]))

(defmethod effects/perform-effects events/navigate-adventure-stylist-gallery
  [dispatch event {:keys [stylist-id query-params] :as args} prev-app-state app-state]
  #?(:cljs
     (api/fetch-stylist-details (get-in app-state storefront.keypaths/api-cache)
                                stylist-id
                                #(do (messages/handle-message events/api-success-fetch-stylist-details %)
                                     (when-let [offset (:offset query-params)]

                                       ;; We wait a moment for the images to at least start to load so that we know where to scroll to.
                                       (js/setTimeout
                                        (fn [] (scroll/scroll-to-selector (str "[data-ref=offset-" offset "]")))
                                        500))))))

(defmethod transitions/transition-state events/navigate-adventure-stylist-gallery
  [_ _ {:keys [stylist-id]} app-state]
  (assoc-in app-state keypaths/stylist-profile-id (spice/parse-int stylist-id)))

(defn built-component
  [data opts]
  (component/build component (query data) {}))
