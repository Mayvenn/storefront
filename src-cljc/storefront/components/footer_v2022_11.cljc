(ns storefront.components.footer-v2022-11
  (:require [spice.date :as date]
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as c]
            [adventure.components.layered :as layered]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui] 
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.platform.component-utils :as utils]
            [storefront.routes :as routes]
            [mayvenn.visual.tools :as vt]
            [storefront.components.accordion-v2022-10 :as accordion]
            [storefront.platform.component-utils :as util]
            [storefront.components.accordions.product-info :as product-info]
            [storefront.accessors.auth :as auth]))

(def ^:private dns-url "https://mayvenn.wirewheel.io/privacy-page/5f22b71054ee7d0012420211")

(c/defcomponent info-accordion-drawer-links-list [{:keys [links row-count column-count]} _ _]
  [:div.grid.gap-4.content-3.p4-on-mb
   {:style {:grid-template-columns (str "repeat(" column-count ", 1fr)")
            :grid-template-rows    (str "repeat(" row-count ", 1fr)")
            :grid-auto-flow        "column"}}
   (map-indexed
    (fn [ix {:keys [target url copy] :as x}]
          [:a.inherit-color.nowrap.overflow-hidden
           (merge
            (when target (apply util/route-to target))
            (when url {:href url})
            {:key   ix
             :style {:text-overflow "ellipsis"}})
           copy]) links)])

(defn ^:private contact-tile
  [icon title copy]
  [:div.flex.flex-column-on-mb.items-center.gap-2
   icon
   [:div.flex.flex-column.items-center-on-mb
    [:div.proxima.content-4 title]
    [:div.proxima.content-2-on-mb.content-3-on-tb-dt copy]]])

(c/defcomponent contact-block
  [{:keys []} _ _]
  [:div.p4-on-mb
   [:div.proxima.content-4.hide-on-tb-dt
    "Have a problem? Need advice on a style or product? Here are a few ways to get a hold of us."]
   [:div.grid.my5.contact-block-tiles
    (contact-tile (svg/customer-service-representative {:style {:height "2.0em"
                                                                :width "2.0em"}})
                  "Monday to Friday"
                  "11am to 8pm ET")
    [:div.border.hide-on-tb-dt]
    (contact-tile (svg/phone-ringing {})
                  "Call Us"
                  (ui/link :link/phone :a.inherit-color {} "+1 (888) 562-7952"))
    (contact-tile (svg/message-bubble {})
                  "Text"
                  (ui/link :link/sms :a.inherit-color {:aria-label "text us at 34649"} "34649"))
    [:div.border.hide-on-tb-dt]
    (contact-tile (svg/mail-envelope {})
                  "email"
                  (ui/link :link/email :a.inherit-color {} "help@mayvenn.com"))]
   [:div.proxima.content-4
    "* Message & data rates may apply. Message frequency varies. See "
    [:a (utils/route-to e/navigate-content-tos) "terms"]
    " & "
    [:a (utils/route-to e/navigate-content-privacy) "privacy policy"]
    "."]])

(c/defcomponent our-locations
  [{:keys []} _ _]
  [:div.grid.p4-on-mb
   (ui/img {:src             "//ucarecdn.com/9d736154-7ec4-4414-9e60-ca4f515d7e55/"
            :alt             ""
            :picture-classes "container-size"})
   [:div.grid.my5.items-center-on-mb.overflow-hidden.footer-our-locations
    [:div.flex.flex-column.items-start-on-tb-dt.items-center-on-mb
     [:div.proxima.content-3.nowrap "Monday-Saturday"]
     [:div.proxima.content-2.nowrap "11am - 7pm"]
     [:div.mt4]
     [:div.proxima.content-3.nowrap "Sunday"]
     [:div.proxima.content-2.nowrap "12pm - 6pm"]]
    [:div.border.container-size.hide-on-tb-dt]
    [:div.flex.flex-column.gap-4.items-center.overflow-hidden.content-3.items-start-on-tb-dt
     (map-indexed
      (fn [ix [evt location]]
        [:a.inherit-color.nowrap (merge (utils/route-to evt)
                                        {:key ix}) location])
      [[e/navigate-retail-walmart-mansfield "Mansfield, TX"]
       [e/navigate-retail-walmart-katy "Katy, TX"]
       [e/navigate-retail-walmart-dallas "Dallas, TX"]
       [e/navigate-retail-walmart-houston "Houston, TX"]
       [e/navigate-retail-walmart-grand-prairie "Grand Prairie, TX"]
       [e/navigate-retail-walmart [:span.medium "See all locations"]]])]]])

(c/defcomponent info-accordion-face-open [{:keys [copy]} _ _]
  [:div.shout.content-3.p4.bold copy])
(c/defcomponent info-accordion-face-closed [{:keys [copy]} _ _]
  [:div.shout.content-3.p4 copy])
(c/defcomponent info-accordion-contents [{:info-accordion.contents/keys [type] :as data} _ _]
  [:div.bg-pale-purple
   (case type
     :faq-accordion (c/build accordion/component
                             (vt/with :footer-faq data)
                             {:opts {:accordion.drawer.open/face-component   product-info/question-open
                                     :accordion.drawer.closed/face-component product-info/question-closed
                                     :accordion.drawer/contents-component    product-info/answer}})
     :links-list    (c/build info-accordion-drawer-links-list data)
     :contact-us    (c/build contact-block data)
     :our-locations (c/build our-locations data)
     [:div.bg-red "CONTENT MISSING!"])])

(defn- social-link
  ([uri icon] (social-link {:height "20px" :width "20px"} uri icon))
  ([{:keys [height width]} uri icon]
   (c/html
    ;; https://web.dev/external-anchors-use-rel-noopener/
    [:a.block.mr4 {:href uri :rel "noopener" :target "_blank"}
     [:div {:style {:width width :height height}}
      ^:inline icon]])))

(c/defcomponent social-media-block
  [_ _ _]
  [:div.px4.py3.flex.items-center
   ^:inline (social-link {:height "28px" :width "28px"} "https://twitter.com/MayvennHair" (svg/mayvenn-on-twitter {:class "fill-p-color"}))
   ^:inline (social-link "http://instagram.com/mayvennhair" (svg/mayvenn-on-instagram {:class "fill-p-color"}))
   ^:inline (social-link "https://www.facebook.com/MayvennHair" (svg/mayvenn-on-facebook {:class "fill-p-color"}))
   ^:inline (social-link "http://www.pinterest.com/mayvennhair/" (svg/mayvenn-on-pinterest {:class "fill-p-color"}))])

(defn ^:private underfoot-link [opts label]
  (c/html [:a.block.inherit-color.my2 opts label]))

(c/defcomponent underfoot
  [{:keys [] :as data} owner opts]
  [:div.white.bg-black.px4.py5.proxima.content-4
   [:div.mx-auto-on-tb-dt.max-960
    ^:inline (svg/mayvenn-text-logo {:height "29px"
                                     :width  "115px"
                                     :class  "fill-white"})
    [:div.flex.gap-6.my3
     ^:inline (underfoot-link (assoc (utils/route-to e/navigate-content-privacy)
                                     :data-test "content-privacy") "Privacy")
     ^:inline (underfoot-link {:href (str (routes/path-for e/navigate-content-privacy) "#ca-privacy-rights")}
                              "CA Privacy Rights")
     ^:inline (underfoot-link {:href      dns-url
                               :data-test "do-not-sell-my-personal-information"}
                              "Do Not Sell My Personal Information")
     ^:inline (underfoot-link (assoc (utils/route-to e/navigate-content-tos)
                                     :data-test "content-tos") "Terms")
     ;; use traditional page load so anchors work
     ^:inline (underfoot-link {:href (str (routes/path-for e/navigate-content-privacy) "#our-ads")} "Our Ads")
     ^:inline (underfoot-link (apply utils/route-to [e/popup-show-return-policy {:location "underfoot"}]) "Return Policy")]
    [:div.flex.items-center {:key "minimal"}
     "©" (date/year (date/now)) " " "Mayvenn"]]])

(c/defcomponent desktop-info-block [{:keys [] :as data} owner opts]
  [:div
   #_[:div.bg-pale-purple
    [:div.container.grid
     {:style {:grid-template-columns "1fr 4fr 1fr"}}
     [:div.shout.title-2.proxima.py4 "FAQs"]
     (c/build accordion/component
              (->> data :drawers (filter #(= "faq" (:id %))) first :contents (vt/with :footer-faq))
              {:opts {:accordion.drawer.open/face-component   product-info/question-open
                      :accordion.drawer.closed/face-component product-info/question-closed
                      :accordion.drawer/contents-component    product-info/answer}})]]
   #_[:div.border-p-color.border.border-width-2]
   [:div.bg-cool-gray
    [:div.container.grid.gap-4.pt4
     {:style {:grid-template-columns "4fr 4fr 2fr 2fr"}}
     [:div
      [:div.shout.title-2.proxima.pb2 "Locations"]
      (c/build our-locations)]
     [:div
      [:div.shout.title-2.proxima.pb2 "Shop"]
      (c/build info-accordion-drawer-links-list (->> data :drawers (filter #(= "shop" (:id %))) first :contents))]
     [:div
      [:div.shout.title-2.proxima.pb2 "About"]
      (c/build info-accordion-drawer-links-list (->> data :drawers (filter #(= "about" (:id %))) first :contents))]
     [:div
      [:div.shout.title-2.proxima.pb2 "Contact Us"]
      (c/build contact-block)]
     [:div.container
      (c/build social-media-block)]]]
   ])

(defn query
  [app-state]
  (merge
   (let [textfield-keypath k/footer-email-value
         email              (get-in app-state textfield-keypath)
         submitted?         (get-in app-state k/footer-email-submitted)
         signed-in?         (auth/signed-in-or-initiated-guest-checkout? app-state)]
     (vt/within :email-capture
                {:id                        (when (not signed-in?) "footer-email-capture")
                 :submit/target             [e/control-footer-email-submit {:email email}]
                 :text-field/id             "footer-email-capture-input"
                 :text-field/placeholder    "Enter your Email"
                 :text-field/focused        (get-in app-state k/ui-focus)
                 :text-field/keypath        textfield-keypath
                 :text-field/errors         (get-in app-state (conj k/field-errors ["email"]))
                 :text-field/email          email
                 :text-field/submitted-text (when submitted? "Thank you for subscribing.")}))
   (accordion/accordion-query
    {:id                :info-accordion
     :allow-all-closed? true
     :allow-multi-open? true
     :open-drawers      (:accordion/open-drawers (accordion/<- app-state :info-accordion))
     :drawers           (remove nil?
                                [#_{:id       "faq"
                                  :face     {:copy "FAQs"}
                                  :contents (merge {:info-accordion.contents/type :faq-accordion}
                                                   (accordion/accordion-query
                                                    {:id                :footer-faq
                                                     :allow-all-closed? true
                                                     :allow-multi-open? false
                                                     :open-drawers      (-> app-state (accordion/<- :footer-faq) :accordion/open-drawers)
                                                     :drawers           (map-indexed (fn [ix {:keys [question answer]}]
                                                                                       {:id       (str "footer-faq-" ix)
                                                                                        :face     {:copy (:text question)}
                                                                                        :contents {:answer answer}})
                                                                                     (-> app-state (get-in (conj k/cms-faq :sitewide-footer)) :question-answers))}))}
                                 {:id       "shop"
                                  :face     {:copy "Shop"}
                                  :contents {:info-accordion.contents/type :links-list
                                             :row-count                    5
                                             :column-count                 2
                                             :links                        [{:target [e/navigate-category
                                                                                      {:page/slug           "wigs"
                                                                                       :catalog/category-id "13"}]
                                                                             :copy   "Wigs"}
                                                                            {:target [e/navigate-category
                                                                                      {:page/slug           "human-hair-bundles"
                                                                                       :catalog/category-id "27"}]
                                                                             :copy   "Hair Bundles"}
                                                                            {:target [e/navigate-category
                                                                                      {:page/slug           "closures"
                                                                                       :catalog/category-id "0"}]
                                                                             :copy   "Closures"}
                                                                            {:target [e/navigate-category
                                                                                      {:page/slug           "frontals"
                                                                                       :catalog/category-id "1"}]
                                                                             :copy   "Frontals"}
                                                                            {:target [e/navigate-category
                                                                                      {:page/slug           "seamless-clip-ins" 
                                                                                       :catalog/category-id "21"}]
                                                                             :copy   "Clip-Ins"}
                                                                            {:target [e/navigate-wigs-101-guide]
                                                                             :copy   "Wigs 101"}
                                                                            {:target [e/navigate-category {:page/slug           "ready-wear-wigs"
                                                                                                           :catalog/category-id "25"}]
                                                                             :copy   "Ready To Wear Wigs"}
                                                                            {:target [e/navigate-landing-page
                                                                                      {:landing-page-slug "new-arrivals"}]
                                                                             :copy   "New Arrivals"}
                                                                            {:target [e/navigate-shop-by-look {:album-keyword :look}]
                                                                             :copy   "Shop by Look"}]}}
                                 {:id       "about"
                                  :face     {:copy "About us"}
                                  :contents {:info-accordion.contents/type :links-list
                                             :row-count                    3
                                             :column-count                 1
                                             :links                        [{:target [e/navigate-content-about-us]
                                                                             :copy   "Our Story"}
                                                                            {:url  "https://shop.mayvenn.com/blog/"
                                                                             :copy "Blog"}
                                                                            {:url  "https://jobs.mayvenn.com/"
                                                                             :copy "Careers"}]}}
                                 {:id       "our-locations"
                                  :face     {:copy "Our Locations"}
                                  :contents {:info-accordion.contents/type :our-locations}}
                                 {:id       "contact-us"
                                  :face     {:copy "Contact Us"}
                                  :contents {:info-accordion.contents/type :contact-us}}])})))

(c/defcomponent component
  [{:keys [] :as data} owner opts]
  [:div
   (c/build layered/lp-divider-purple-pink)
   [:div.hide-on-tb-dt
    (c/build accordion/component
             (vt/with :info-accordion data)
             {:opts
              (vt/within :accordion.drawer
                         {:open/face-component   info-accordion-face-open
                          :closed/face-component info-accordion-face-closed
                          :contents-component    info-accordion-contents})})
    [:div.container
     (c/build social-media-block)]]
   [:div.hide-on-mb
    (c/build desktop-info-block
             (vt/with :info-accordion data))]
   (c/build underfoot (vt/with :underfoot data) opts)])
