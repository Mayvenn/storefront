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
        stylist    (stylists/by-id data stylist-id)
        back       (first (get-in data storefront.keypaths/navigation-undo-stack))]
    (when stylist
      ;; TODO: Use new header
      {:header-data {:subtitle     [:div.h4.medium
                                    (str (stylists/->display-name stylist) "'s Recent work" )]
                     :header-attrs {:class "bg-too-light-lavender black"}
                     :right-corner {:id    "close-stylist-gallery"
                                    :opts (utils/route-back-or-to back
                                                                  events/navigate-adventure-stylist-profile
                                                                  {:stylist-id stylist-id
                                                                   :store-slug (:store-slug stylist)})
                                    :value (svg/simple-x {:class "dark-gray"
                                                          :style {:width  "20px"
                                                                  :height "20px"}})}}
       :gallery     (map (comp ui/ucare-img-id :resizable-url) (:gallery-images stylist))})))

(defn ^:private component-overhead-magic-number [mobile-overhead desktop-overhead]
  #?(:cljs
     (if (< (.-width (goog.dom/getViewportSize)) 750)
       mobile-overhead
       desktop-overhead)))

(defn ^:private handle-scroll [component event]
  #?(:cljs
     (let [{:keys [mobile-overhead desktop-overhead]} (component/get-opts component)]
       (component/set-state! component
                             :show? (< (component-overhead-magic-number mobile-overhead desktop-overhead)
                                       (.-y (goog.dom/getDocumentScroll)))))))

(defn ^:private set-height [component dom-node]
  #?(:cljs
     (component/set-state! component :component-height (some-> dom-node goog.style/getSize .-height))))

(defdynamic-component sticky-organism
  (constructor [this]
               (component/create-ref! this "header")
               {:show? false :component-height 0})
  (did-mount [this]
             (set-height this (component/get-ref this "header"))
             #?(:cljs
                (goog.events/listen js/window EventType/SCROLL (partial handle-scroll this))))

  (will-unmount [this]
                #?(:cljs
                   (goog.events/unlisten js/window EventType/SCROLL (partial handle-scroll this))))
  ;; (did-update [this _ _ _] (set-height this (component/get-ref this "header")))
  (render [this]
          (let [{:keys [child data]}             (component/get-opts this)
                {:keys [show? component-height]} (component/get-state this)]
            (component/html
             [:div.fixed.z4.top-0.left-0.right-0
              (if show?
                {:style {:margin-top "0px"}
                 :class "transition-2"}
                {:class "hide"
                 :style {:margin-top (str "-" component-height "px")}})
              [:div.col-12 {:ref (component/use-ref this "header")}
               (component/build child data nil)]]))))

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
