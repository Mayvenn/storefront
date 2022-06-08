(ns adventure.stylist-matching.stylist-gallery
  (:refer-clojure :exclude [name])
  (:require #?@(:cljs
                [[goog.dom]
                 [goog.events.EventType :as EventType]
                 [goog.events]
                 [goog.style]
                 [storefront.browser.scroll :as scroll]
                 [storefront.platform.messages :refer [handle-message]]])
            api.stylist
            [storefront.accessors.sites :as sites]
            [storefront.components.header :as header]
            [adventure.keypaths :as keypaths]
            [spice.core :as spice]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [storefront.components.gallery :as gallery]
            storefront.keypaths
            [storefront.platform.component-utils :as utils]
            [storefront.transitions :as transitions]))

(defn query
  [data]
  (let [{:as                   detailed-stylist
         :stylist/keys         [id name slug]
         :stylist.gallery/keys [images]}
        (api.stylist/by-id data (get-in data keypaths/stylist-profile-id))
        back (first (get-in data storefront.keypaths/navigation-undo-stack))]
    (when detailed-stylist
      {:stylist-gallery-header/title       (str name "'s Recent work")
       :stylist-gallery-header/close-id    "close-stylist-gallery"
       :stylist-gallery-header/close-route (utils/route-back-or-to back
                                                                   e/navigate-adventure-stylist-profile
                                                                   {:stylist-id id
                                                                    :store-slug slug})
       :gallery                            (map :resizable-url images)})))

(defn ^:private component-overhead-magic-number [mobile-overhead desktop-overhead]
  #?(:cljs
     (if (< (.-width (goog.dom/getViewportSize)) 750)
       mobile-overhead
       desktop-overhead)))

(defn ^:private handle-scroll [component event]
  #?(:cljs
     (let [{:component/keys [mobile-overhead desktop-overhead]} (component/get-opts component)]
       (component/set-state! component
                             :show? (< (component-overhead-magic-number mobile-overhead desktop-overhead)
                                       (.-y (goog.dom/getDocumentScroll)))))))

(defn stylist-gallery-header-molecule
  [{:stylist-gallery-header/keys [title close-id close-route]}]
  [:div
   [:div.fixed.z4.top-0.left-0.right-0
    (header/nav-header
     {:class "border-bottom border-gray bg-white black"
      :style {:height "70px"}}
     nil
     (component/html [:div.h4.medium.center title])
     (component/html
      (when close-id
        [:a.block.ml-auto.flex.items-center.justify-around
         (merge {:data-test close-id
                 :style     {:width  "70px"
                             :height "70px"}}
                close-route)
         (svg/x-sharp {:class "black"
                       :style {:width  "14px"
                               :height "14px"}})])))]
   [:div {:style {:margin-top "70px"}}]])

(defn desktop-stylist-gallery-header-molecule
  [{:stylist-gallery-header/keys [title close-id close-route]}]
  [:div
   [:div.fixed.z4.top-0.left-0.right-0
    (header/nav-header
     {:class "border-bottom border-gray bg-white black"
      :style {:height "70px"}}
     nil
     (component/html [:div.h4.medium title])
     (component/html
      (when close-id
        [:div.ml-auto.flex.items-center.justify-around
         (merge {:data-test close-id
                 :style     {:width  "70px"
                             :height "70px"}}
                close-route)
         (svg/x-sharp {:class "black"
                       :style {:width  "14px"
                               :height "14px"}})])))]
   [:div {:style {:margin-top "70px"}}]])

(defn ^:private stylist-gallery-image
  [idx url]
  ;; padding hack to preserve pre-load image aspect ratio
  [:div.relative {:style {:padding-top "100%"}}
   [:picture.absolute
    {:data-ref (str "offset-" idx)
     :style    {:top 0
                :bottom 0}}
    [:source {:src-set
              (str url "-/scale_crop/1160x1160/smart/ 2x," url "-/scale_crop/580x580/smart/ 1x")}]
    [:img {:src   (str url "-/scale_crop/580x580/smart/")
           :style {:width "100%"}}]]])

(defn ^:private desktop-stylist-gallery-image
  [idx url]
  ;; padding hack to preserve pre-load image aspect ratio
  [:picture.ml1
   {:data-ref (str "offset-" idx)
    :style    {:top    0
               :bottom 0}}
   [:source {:src-set
             (str url "-/scale_crop/1160x1160/smart/ 2x," url "-/scale_crop/580x580/smart/ 1x")}]
   [:img {:src   (str url "-/scale_crop/580x580/smart/")
          :style {:width "32%"}
          :alt   "stylist's preveious work"}]])

(defcomponent component
  [data owner opts]
  [:div
   [:div.col-12.bg-white.hide-on-tb-dt
    (stylist-gallery-header-molecule data)
    (map-indexed stylist-gallery-image (:gallery data))]
   [:div.col-12.bg-white.hide-on-mb
    (desktop-stylist-gallery-header-molecule data)
    [:div.flex-wrap.ml3
     (map-indexed desktop-stylist-gallery-image (:gallery data))]]])

(defmethod transitions/transition-state e/navigate-adventure-stylist-gallery
  [_ _ {:keys [stylist-id]} state]
  ;; NOTE this complects stylist-profile with gallery
  (assoc-in state keypaths/stylist-profile-id (spice/parse-int stylist-id)))

(defmethod fx/perform-effects e/navigate-adventure-stylist-gallery
  [_ _ {:keys [stylist-id query-params] :as args} _ state]
  (if (not= :shop (sites/determine-site state))
    (fx/redirect e/navigate-home)
    #?(:cljs
       (handle-message e/cache|stylist|requested
                       {:stylist/id stylist-id
                        :on/success
                        #(when-let [offset (:offset query-params)]
                           ;; We wait a moment for the images to at least start to load so that we know where to scroll to.
                           (js/setTimeout
                            (fn [] (scroll/scroll-to-selector (str "[data-ref=offset-" offset "]")))
                            500))
                        :on/failure
                        (fn [] (handle-message e/flash-later-show-failure
                                               {:message
                                                (str "The stylist you are looking for is not available. "
                                                     "Please search for another stylist in your area below. ")})
                          (fx/redirect e/navigate-adventure-find-your-stylist))}))))

(defn ^:export built-component
  [data _]
  (component/build component (query data) {}))
