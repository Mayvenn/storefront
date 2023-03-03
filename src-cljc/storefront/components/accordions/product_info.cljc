(ns storefront.components.accordions.product-info
  (:require [storefront.component :as c]
            [storefront.components.accordion-v2022-10 :as accordion]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [mayvenn.visual.tools :refer [with within]]
            [markdown-to-hiccup.core :as markdown]))

(c/defcomponent question-open [{:keys [copy]} _ _]
  [:div.content-3.px2.py4.bold copy])
(c/defcomponent question-closed [{:keys [copy]} _ _]
  [:div.content-3.px2.py4 copy])
(c/defcomponent answer [{:keys [answer]} _ _]
  (map-indexed (fn [i {blocks :paragraph}]
                 [:div.p2.bg-cool-gray {:key (str "paragraph-" i)}
                  (map-indexed (fn [j {:keys [text url]}]
                                 (if url
                                   [:a.p-color {:href url :key (str "text-" j)} text]
                                   [:span {:key (str "text-" j)} text]))
                               blocks)])
               answer))

(c/defcomponent face-open [{:keys [copy]} _ _]
  [:div.shout.content-3.px2.py4.bold copy])
(c/defcomponent face-closed [{:keys [copy]} _ _]
  [:div.shout.content-3.px2.py4 copy])
(c/defcomponent contents [{:as data :keys [id primary sections faq]} _ _]
  [:div.bg-cool-gray
   {:key (str id "-tab")}
   [:div primary]
   (when (seq sections)
     [:div.flex.flex-wrap.justify-between.p2.pdp-details-accordion-section
      (map-indexed
       (fn section [idx {:keys [content] :as section}]
         [:div.my2.pr2
          {:style {:min-width "50%"}
           :key   idx}
          (conj [:div]
                (if (string? content)
                  (markdown/component (markdown/md->hiccup content))
                  content))
          (when-let [link-content (:link/content section)]
            (ui/button-small-underline-primary
             (assoc
              (apply utils/fake-href (:link/target section))
              :data-test (:link/id section))
             link-content))])
       sections)])
   (when (not-empty faq)
     [:div.flex-auto.bg-pale-purple
      (c/build accordion/component
               (with :pdp-faq faq)
               {:opts
                {:accordion.drawer.open/face-component   question-open
                 :accordion.drawer.closed/face-component question-closed
                 :accordion.drawer/contents-component    answer}})])])
