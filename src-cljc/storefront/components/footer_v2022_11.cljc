(ns storefront.components.footer-v2022-11
  (:require [spice.date :as date]
            [storefront.component :as c]
            [adventure.components.layered :as layered]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.platform.component-utils :as utils]
            [storefront.routes :as routes]
            [mayvenn.visual.tools :as vt]
            [homepage.ui.email-capture :as email-capture]
            [storefront.components.accordion-v2022-10 :as accordion]
            [storefront.platform.component-utils :as util]))

(c/defcomponent info-accordion-drawer-links-list [{:keys [links row-count column-count]} _ _]
  [:div.p4.grid.gap-4
   {:style {:grid-template-columns (str "repeat(" column-count ", 1fr)")
            :grid-template-rows    (str "repeat(" row-count ", 1fr)")
            :grid-auto-flow        "column"}}
   (map (fn [{:keys [target url copy]}]
          [:a.inherit-color
           (merge
            (when target (apply util/route-to target))
            (when url {:href url}))
           copy]) links)])

(c/defcomponent info-accordion-face-open [{:keys [copy]} _ _]
  [:div.shout.content-3.p4.bold copy])
(c/defcomponent info-accordion-face-closed [{:keys [copy]} _ _]
  [:div.shout.content-3.p4 copy])
(c/defcomponent info-accordion-contents [{:info-accordion.contents/keys [type] :as data} _ _]
  [:div.bg-pale-purple
   (case type
     :faq-accordion [:div "faq accordion, yo!"]#_(c/build accordion/component data)
     :links-list    (c/build info-accordion-drawer-links-list data)
     :contact-us    [:div "contacty"]
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

(c/defcomponent essence-block
  [{:keys [copy]} owner opts]
  [:div.px4.pb3.proxima.content-4.dark-gray copy])

(defn- underfoot-link [opts label]
  (c/html [:a.block.inherit-color.my2 opts label]))

(c/defcomponent underfoot
  [{:keys [] :as data} owner opts]
  [:div.white.bg-black.px4.py5.proxima.content-4
   ^:inline (svg/mayvenn-text-logo {:height "29px"
                                    :width  "115px"
                                    :class  "fill-white"})
   [:div.flex.justify-between.my3
    ^:inline (underfoot-link (assoc (utils/route-to e/navigate-content-privacy)
                                    :data-test "content-privacy") "Privacy")
    ^:inline (underfoot-link {:href (str (routes/path-for e/navigate-content-privacy) "#ca-privacy-rights")}
                             "CA Privacy Rights")
    ^:inline (underfoot-link (assoc (utils/route-to e/navigate-content-tos)
                                    :data-test "content-tos") "Terms")
    ;; use traditional page load so anchors work
    ^:inline (underfoot-link {:href (str (routes/path-for e/navigate-content-privacy) "#our-ads")} "Our Ads")]
   [:div.flex.items-center {:key "minimal"}
    "©" (date/year (date/now)) " " "Mayvenn"]])

(defn query
  [app-state]
  (merge
   (let [textfield-keypath k/footer-email-value
         email             (get-in app-state textfield-keypath)
         submitted?        (get-in app-state k/footer-email-submitted)]
     (vt/within :email-capture
                {:submit/target             [e/control-footer-email-submit {:email email}]
                 :text-field/id             "footer-email-capture-input"
                 :text-field/placeholder    "Enter your Email"
                 :text-field/focused        (get-in app-state k/ui-focus)
                 :text-field/keypath        textfield-keypath
                 :text-field/errors         (get-in app-state (conj k/field-errors ["email"]))
                 :text-field/email          email
                 :text-field/submitted-text (when submitted? "Thank you for subscribing.")}))
   {:essence-block/copy "Included is a one year subscription to ESSENCE Magazine - a $10 value! Offer and refund details will be included with your confirmation."}
   #:underfoot{} ; GROT?
   (accordion/accordion-query
    {:id                :info-accordion
     :allow-all-closed? true
     :allow-multi-open? true
     :open-drawers      (:accordion/open-drawers (accordion/<- app-state :info-accordion))
     :drawers           [{:id       "faq"
                          :face     {:copy "FAQs"}
                          :contents {:info-accordion.contents/type :faq-accordion}}
                         {:id       "shop"
                          :face     {:copy "Shop"}
                          :contents {:info-accordion.contents/type :links-list}}
                         {:id       "about"
                          :face     {:copy "About"}
                          :contents {:info-accordion.contents/type :links-list
                                     :row-count                    3
                                     :column-count                 1
                                     :links                        [{:target [e/navigate-content-about-us]
                                                                     :copy   "Our Story"}
                                                                    {:copy   "Events [LINK TARGET MISSING]"}
                                                                    {:url  "https://shop.mayvenn.com/blog/"
                                                                     :copy "Blog"}]}}
                         {:id       "our-locations"
                          :face     {:copy "Our Locations"}
                          :contents {}}
                         {:id       "contact-us"
                          :face     {:copy "Contact Us"}
                          :contents {:info-accordion.contents/type :contact-us}}
                         {:id       "careers"
                          :face     {:copy "Carrers"}
                          :contents {}}]})))

(c/defcomponent component
  [{:keys [] :as data} owner opts]
  [:div
   (c/build email-capture/organism data) ; TODO: use with/within to avoid rerenders?
   (c/build layered/lp-divider-purple-pink)
   (c/build accordion/component
            (vt/with :info-accordion data)
            {:opts
             (vt/within :accordion.drawer
                        {:open/face-component   info-accordion-face-open
                         :closed/face-component info-accordion-face-closed
                         :contents-component    info-accordion-contents})})
   (c/build social-media-block)
   (c/build essence-block (vt/with :essence-block data))
   (c/build underfoot (vt/with :underfoot data) opts)])
