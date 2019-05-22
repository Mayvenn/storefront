(ns adventure.informational.certified-stylists
  (:require [clojure.string :as string]
            storefront.keypaths
            [storefront.accessors.pixlee :as pixlee]
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
                       [storefront.browser.scroll :as scroll]
                       [storefront.hooks.pixlee :as pixlee.hook]])))

(defn ^:private ->freeinstall-nav-event [source path]
  [events/external-redirect-freeinstall
   {:query-string (string/join
                   "&"
                   ["utm_medium=referral"
                    (str "utm_source=" source)
                    "utm_term=fi_shoptofreeinstall"])
    :path         path}])

(defn query
  [data]
  {:layers [{:layer/type :hero
             :photo/uuid "fed7867a-4b32-44d2-b095-81274620c8eb"}
            {:layer/type   :find-out-more
             :react/key    "certified-stylists"
             ;;:section/opts  {:class "bg-white"}
             :header/value "Our Certified Stylists are the best of the best."
             :body/value   (str "Our Certified Stylists are the best in your area. "
                                "They’re chosen because of their top-rated reviews, professionalism, and amazing work.")
             :cta/value    "Get started"
             :cta/event    (->freeinstall-nav-event "toadventurehomepagestylistinfopage"
                                                    "/adv/install-type")}

            {:layer/type      :bulleted-explainer
             :header/value    "About Our Certified Stylists"
             :subheader/value "An overview"

             :bullets [{:icon/uuid    "6f63157c-dc3a-4bbb-abcf-e03b08d6e102"
                        :icon/width   "27"
                        :header/value "Licensed Stylists"
                        :body/value   "Our certified stylists are licensed, regulated service practitioners in each state."}
                       {:icon/uuid    "deeaa11d-c48a-4657-8d01-f477d2ea18a5"
                        :icon/width   "24"
                        :header/value "Salon Professionals"
                        :body/value   (str "We believe that quality beauty should be accessible for all. "
                                           "Our stylists work in professional, clean salon spaces.")}
                       {:icon/uuid    "b862f042-ad8d-4230-8ce4-20059dd540d7"
                        :icon/width   "34"
                        :header/value "Top Ratings & Reviews"
                        :body/value   (str "From Yelp to client surveys, we ensure that our stylists adhere"
                                           " to a top-notch code of ethics and professionalism.")}]}
            {:layer/type      :ugc
             :header/value    "#MayvennFreeInstall"
             :subheader/value "Showcase your new look by tagging #MayvennFreeInstall"
             :images          (pixlee/images-in-album
                               (get-in data storefront.keypaths/ugc)
                               :free-install-mayvenn)}
            {:layer/type     :faq
             :expanded-index (get-in data storefront.keypaths/faq-expanded-section)
             :sections       [{:title      "Who is going to do my hair?",
                               :paragraphs ["Mayvenn Certified Stylists have been chosen because of their professionalism, skillset, and client ratings. We’ve got a network of licensed stylists across the country who are all committed to providing you with amazing service and quality hair extensions."]}
                              {:title      "What kind of hair do you offer?",
                               :paragraphs ["We’ve got top of the line virgin hair in 8 different textures. In the event that you’d like to switch it up, we have pre-colored options available as well. The best part? All of our hair is quality-guaranteed."]}
                              {:title      "What happens after I choose my hair?",
                               :paragraphs ["After you choose your hair, you’ll be matched with a Certified Stylist of your choice. You can see the stylist’s work and their salon’s location. We’ll help you book an appointment and answer any questions you may have."]}
                              {:title      "Is Mayvenn Install really a better deal?",
                               :paragraphs ["Yes! It’s basically hair and service for the price of one. You can buy any 3 bundles (closures and frontals included) from Mayvenn, and we’ll pay for you to get your hair installed by a local stylist. That means that you’re paying $0 for your next sew-in, with no catch!"]}
                              {:title      "How does this process actually work?",
                               :paragraphs ["It’s super simple — after you purchase your hair, we’ll send you a pre-paid voucher that you’ll use during your appointment. When your stylist scans it, they get paid instantly by Mayvenn."]}
                              {:title      "What if I want to get my hair done by another stylist? Can I still get the free install?",
                               :paragraphs ["You must get your hair done from a Certified Stylist in order to get your hair installed for free."]}] }
            {:layer/type :contact}
            {:layer/type :sticky-footer
             :cta/event  (->freeinstall-nav-event "toadventurehomepagestylistinfopage"
                                                  "/adv/install-type")}]})


(defmulti layer-view (fn [{:keys [layer/type]} _ _] type))

(defmethod layer-view :hero
  [data owner opts]
  (component/create
   (ui/ucare-img {:class "col-12"} (:photo/uuid data))))

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

    (let [{:cta/keys [event value]} data]
      (when (and event value)
        [:a.block.h3.medium.teal.my2.flex.items-center
         (apply utils/fake-href event)
         value
         [:div.flex.items-end.ml2 {:style {:transform "rotate(-90deg)"}}
          (svg/dropdown-arrow {:class  "stroke-teal"
                               :style  {:stroke-width "3px"}
                               :height "14px"
                               :width  "14px"})]]))]))

(defmethod layer-view :bulleted-explainer
  [data owner opts]
  (let [step (fn [{:as point
                   :keys [icon/uuid icon/width]}]
               [:div.col-12.mt2.center.col-4-on-dt
                [:div.flex.justify-center.items-end.my2
                 {:style {:height "39px"}}
                 (ui/ucare-img {:alt (:header/title point) :width width} uuid)]
                [:div.h5.medium.mb1 (:header/value point)]
                [:p.h6.col-10.col-9-on-dt.mx-auto.dark-gray (:body/value point)]])]

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

(defn ^:private redirect-to-freeinstall
  [source]
  (utils/fake-href events/external-redirect-freeinstall
                   {:query-string (string/join
                                   "&"
                                   ["utm_medium=referral"
                                    (str "utm_source=" source)
                                    "utm_term=fi_shoptofreeinstall"])}))

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
           (let [{:cta/keys [event]} data]
             (component/html
              [:div.hide-on-dt
               {:class "fixed"}
               ;; padding div to allow content that's normally at the bottom to be visible
               [:div {:style {:height (str content-height "px")}}]
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
                                            (apply utils/fake-href event))
                                     [:div.h7 "Get started"])]]]]]]])))))))

(defmethod layer-view :default [_ _ _] (component/create [:div]))

(defn component [{:keys [layers]} owner opts]
  (component/create
   (into [:div]
         (comp
          (map (fn [layer-data]
                 [:section
                  (component/build layer-view layer-data opts)])))
         layers)))

(defn built-component
  [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-info-certified-stylists
  [_ _ args prev-app-state app-state]
  #?(:cljs (pixlee.hook/fetch-album-by-keyword :free-install-mayvenn)))
