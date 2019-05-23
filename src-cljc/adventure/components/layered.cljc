(ns adventure.components.layered
  (:require [clojure.string :as string]
            storefront.keypaths
            [storefront.component :as component]
            [storefront.components.accordion :as accordion]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            adventure.home
            #?@(:cljs [[om.core :as om]
                       [goog.events.EventType :as EventType]
                       goog.dom
                       goog.style
                       goog.events
                       [storefront.browser.scroll :as scroll]])))

(defn ->freeinstall-nav-event [source path]
  [events/external-redirect-freeinstall
   {:query-string (string/join
                   "&"
                   ["utm_medium=referral"
                    (str "utm_source=" source)
                    "utm_term=fi_shoptofreeinstall"])
    :path         path}])

(defmulti layer-view (fn [{:keys [layer/type]} _ _] type))

(defmethod layer-view :hero
  [data owner opts]
  (component/create
   (ui/ucare-img {:class "col-12"} (:photo/uuid data))))

(defn ^:private cta-with-chevron
  [{:cta/keys [navigation-message value]}]
  (when (and navigation-message value)
    [:a.block.h3.medium.teal.my2
     (apply utils/route-to navigation-message)
     value
     (svg/dropdown-arrow {:class  "stroke-teal ml2"
                          :style  {:stroke-width "3px"
                                   :transform "rotate(-90deg)"}
                          :height "14px"
                          :width  "14px"})]))

(defmethod layer-view :find-out-more
  [data owner opts]
  (component/create
   [:div.col-12.bg-white.py8.flex.flex-column.items-center.justify-center.center
    (:section/opts data)

    (let [{:header/keys [value]} data]
      (when value
        [:div.col-10.my2.h2 value]))

    (let [{:body/keys [value]} data]
      (when value
        [:div.col-10.my2.h5.dark-gray value]))

    (cta-with-chevron data)]))

(defmethod layer-view :bulleted-explainer
  [data owner opts]
  (let [step (fn [{:as point
                   :keys [icon/uuid icon/width]}]
               [:div.col-12.mt2.center.col-4-on-dt
                [:div.flex.justify-center.items-end.my2
                 {:style {:height "39px"}}
                 (ui/ucare-img {:alt (:header/title point) :width width} uuid)]
                [:div.h5.medium.mb1 (:header/value point)]
                [:p.h6.col-10.col-9-on-dt.mx-auto.dark-gray (:body/value point)]
                (cta-with-chevron point)])]

    (component/create
     [:div.col-12.py10.bg-transparent-teal
      [:div.mt2.flex.flex-column.items-center
       (let [{:header/keys [value]} data]
         [:h2 value])

       (let [{:subheader/keys [value]} data]
         [:div.h6.dark-gray value])]

      (into
       [:div.col-8-on-dt.mx-auto.flex.flex-wrap]
       (comp (map step))
       (:bullets data))])))

(defmethod layer-view :ugc
  [data owner opts]
  (component/create
   [:div.py8.col-10.mx-auto
    (let [{:header/keys [value]} data]
      [:h2.center value])
    (let [{:subheader/keys [value]} data]
      [:h6.center.dark-gray value])
    [:div.flex.flex-wrap.pt2
     (for [{:keys [imgs]} (:images data)]
       [:a.col-6.col-3-on-tb-dt.p1
        (ui/aspect-ratio
         1 1
         [:img {:class "col-12"
                :src   (-> imgs :original :src)}])])]]))

(defmethod layer-view :faq
  [{:keys [expanded-index sections]} owner opts]
  (component/create
   [:div.px6.mx-auto.col-6-on-dt
    [:h2.center "Frequently Asked Questions"]
    (component/build
     accordion/component
     {:expanded-indices #{expanded-index}
      :sections         (map
                         (fn [{:keys [title paragraphs]}]
                           {:title [:h6 title]
                            :paragraphs paragraphs})
                         sections)}
     {:opts {:section-click-event events/faq-section-selected}})]))

(defmethod layer-view :contact
  [_ _ _]
  (component/create adventure.home/contact-us))

(defmethod layer-view :sticky-footer
  [data owner opts]
  #?(:clj (component/create [:div])
     :cljs
     (letfn [(handle-scroll [e] (om/set-state! owner
                                               :show?
                                               (< 530
                                                  (.-y (goog.dom/getDocumentScroll)))))
             (set-height [] (om/set-state! owner
                                           :content-height
                                           (some-> owner
                                                   (om/get-node "content-height")
                                                   goog.style/getSize
                                                   .-height)))]
       (reify
         om/IInitState
         (init-state [this]
           {:show?          false
            :content-height 0})
         om/IDidMount
         (did-mount [this]
           (handle-scroll nil) ;; manually fire once on load incase the page already scrolled
           (set-height)
           (goog.events/listen js/window EventType/SCROLL handle-scroll))
         om/IWillUnmount
         (will-unmount [this]
           (goog.events/unlisten js/window EventType/SCROLL handle-scroll))
         om/IWillReceiveProps
         (will-receive-props [this next-props]
           (set-height))
         om/IRenderState
         (render-state [this {:keys [show? content-height]}]
           (let [{:cta/keys [message]} data]
             (component/html
              [:div.hide-on-dt
               [:div.fixed.z4.bottom-0.left-0.right-0
                {:style {:margin-bottom (str "-" content-height "px")}}
                ;; Using a separate element with reverse margin to prevent the
                ;; sticky component from initially appearing on the page and then
                ;; animate hiding.
                [:div.transition-2
                 (if show?
                   {:style {:margin-bottom (str content-height "px")}}
                   {:style {:margin-bottom "0"}})
                 [:div {:ref "content-height"}
                  [:div
                   [:div.h6.white.bg-black.medium.px3.py6.flex.items-center
                    [:div.col-7 "We can't wait to pay for your install!"]
                    [:div.col-1]
                    [:div.col-4
                     (ui/teal-button (merge {:height-class "py2"
                                             :data-test    "sticky-footer-get-started"}
                                            (apply utils/route-to message))
                                     [:div.h7 "Get started"])]]]]]]])))))))

(defmethod layer-view :default
  [data _ _]
  (component/create [:div.bg-red.center.border.border-width-3.border-light-teal.p4
                     (str (:layer/type data))]))

(defn component [{:keys [layers]} owner opts]
  (component/create
   (into [:div]
         (comp
          (map (fn [layer-data]
                 [:section
                  (component/build layer-view layer-data opts)])))
         layers)))

