(ns storefront.components.slideout-nav
  (:require [storefront.platform.component-utils :as utils]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.events :as events]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :as messages]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.stylists :refer [own-store? community-url]]
            [storefront.components.money-formatters :refer [as-money]]
            [storefront.routes :as routes]
            [clojure.string :as str]
            [storefront.platform.component-utils :as utils]
            [storefront.components.header :as header]
            [storefront.assets :as assets]
            [storefront.components.promotion-banner :as promotion-banner]))

(def menu-x
  (component/html
   [:div.absolute {:style {:width "60px"}}
    [:div.relative.rotate-45.p2 {:style {:height "60px"}
                                 :on-click #(messages/handle-message events/control-menu-collapse-all)}
     [:div.absolute.border-right.border-dark-gray {:style {:width "18px" :height "36px"}}]
     [:div.absolute.border-bottom.border-dark-gray {:style {:width "36px" :height "18px"}}]]]))

(defn logo [height]
  (component/html
   [:a.block.img-logo.bg-no-repeat.bg-center.bg-contain.teal.pp3
    (merge {:style {:height height}
            :title "Mayvenn"
            :item-prop "logo"
            :data-test "header-logo"
            :content (str "https:" (assets/path "/images/header_logo.svg"))}
           (utils/route-to events/navigate-home))]))

(defn component [data owner opts]
  (component/create
   [:div
    [:div.fixed.top-0.left-0.right-0.z4.bg-white
     (promotion-banner/built-component data opts)
     [:div.border-bottom.border-gray.mx-auto
      {:style {:max-width "1440px"}}
      menu-x
      [:div.center.col-12.px3.py2 {:style {:min-width "251px"}}
       (logo "40px")]]]
    [:div
     "Lorem ipsum dolor sit amet, usu eu tollit numquam, ea nibh adolescens sea. Cu quo vide mandamus forensibus, mea velit eligendi ne, pro platonem concludaturque ad. Choro gubergren ex vis. At pri minim prompta maiorum. Ei tation postulant voluptatibus qui, ornatus expetenda ad sit, id quo suscipit gubergren.

Cu odio dictas omittam mel, blandit consequuntur ea eos. Qui putent epicuri quaerendum eu, in molestie appareat usu. Nibh definiebas eu sed, cum te vitae maiorum inimicus. Euismod eligendi complectitur ex has, no dicat viderer expetendis pri, vim no eirmod maiestatis.

An tation efficiantur quo. Pro et quis liber, facilisi convenire vituperata ne sea, qui ei laoreet apeirian quaestio. Ei pri malis blandit, dicit causae labitur ad vix, posse recusabo vix ne. An aliquid assentior vel. Ut causae expetenda efficiendi quo, alterum meliore vivendo no per.

Ne eum saepe tantas. Ut ius doming imperdiet expetendis, putant fierent incorrupte te eos. At denique omittantur sed, no vidit soluta vulputate nec. Nominavi ocurreret iracundia vix ex, est accusamus tincidunt at, his augue commune appareat in. Autem errem eos et, cum posse molestie at, adhuc dolores posidonium cu mel.

Diam philosophia in nec, sed vero posse eu, cu eam fabulas corpora petentium. Appetere adipiscing ne eam, ne mea simul pertinax. Primis quodsi similique ei nec. Ad mentitum sadipscing omittantur his, clita adversarium definitiones duo no, ad eum choro praesent."]
    [:div
     "Lorem ipsum dolor sit amet, usu eu tollit numquam, ea nibh adolescens sea. Cu quo vide mandamus forensibus, mea velit eligendi ne, pro platonem concludaturque ad. Choro gubergren ex vis. At pri minim prompta maiorum. Ei tation postulant voluptatibus qui, ornatus expetenda ad sit, id quo suscipit gubergren.

Cu odio dictas omittam mel, blandit consequuntur ea eos. Qui putent epicuri quaerendum eu, in molestie appareat usu. Nibh definiebas eu sed, cum te vitae maiorum inimicus. Euismod eligendi complectitur ex has, no dicat viderer expetendis pri, vim no eirmod maiestatis.

An tation efficiantur quo. Pro et quis liber, facilisi convenire vituperata ne sea, qui ei laoreet apeirian quaestio. Ei pri malis blandit, dicit causae labitur ad vix, posse recusabo vix ne. An aliquid assentior vel. Ut causae expetenda efficiendi quo, alterum meliore vivendo no per.

Ne eum saepe tantas. Ut ius doming imperdiet expetendis, putant fierent incorrupte te eos. At denique omittantur sed, no vidit soluta vulputate nec. Nominavi ocurreret iracundia vix ex, est accusamus tincidunt at, his augue commune appareat in. Autem errem eos et, cum posse molestie at, adhuc dolores posidonium cu mel.

Diam philosophia in nec, sed vero posse eu, cu eam fabulas corpora petentium. Appetere adipiscing ne eam, ne mea simul pertinax. Primis quodsi similique ei nec. Ad mentitum sadipscing omittantur his, clita adversarium definitiones duo no, ad eum choro praesent."]
    [:div
     "Lorem ipsum dolor sit amet, usu eu tollit numquam, ea nibh adolescens sea. Cu quo vide mandamus forensibus, mea velit eligendi ne, pro platonem concludaturque ad. Choro gubergren ex vis. At pri minim prompta maiorum. Ei tation postulant voluptatibus qui, ornatus expetenda ad sit, id quo suscipit gubergren.

Cu odio dictas omittam mel, blandit consequuntur ea eos. Qui putent epicuri quaerendum eu, in molestie appareat usu. Nibh definiebas eu sed, cum te vitae maiorum inimicus. Euismod eligendi complectitur ex has, no dicat viderer expetendis pri, vim no eirmod maiestatis.

An tation efficiantur quo. Pro et quis liber, facilisi convenire vituperata ne sea, qui ei laoreet apeirian quaestio. Ei pri malis blandit, dicit causae labitur ad vix, posse recusabo vix ne. An aliquid assentior vel. Ut causae expetenda efficiendi quo, alterum meliore vivendo no per.

Ne eum saepe tantas. Ut ius doming imperdiet expetendis, putant fierent incorrupte te eos. At denique omittantur sed, no vidit soluta vulputate nec. Nominavi ocurreret iracundia vix ex, est accusamus tincidunt at, his augue commune appareat in. Autem errem eos et, cum posse molestie at, adhuc dolores posidonium cu mel.

Diam philosophia in nec, sed vero posse eu, cu eam fabulas corpora petentium. Appetere adipiscing ne eam, ne mea simul pertinax. Primis quodsi similique ei nec. Ad mentitum sadipscing omittantur his, clita adversarium definitiones duo no, ad eum choro praesent."]]))

(defn query [data]
  (merge
   (promotion-banner/query data)
   {}))

(defn built-component [data opts]
  (component/build component (query data) nil))
