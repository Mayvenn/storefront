(ns homepage.ui.omni-split-image-cta
  (:require [storefront.component :as c]
            [storefront.events :as e]
            [storefront.components.ui :as ui]))


(c/defcomponent organism
  [{:keys [id]} _ _]
  (let [img-url "http://placekitten.com/800/800?image=2"
        img-alt "[Lorem Ipsum] This is how to cat"
        primary "[Lorem Ipsum]"
        secondary (str
                   "It's a basket o' tiny cats! I need to write more stuff here so that the "
                   "layout looks correct despite the content being gibberish. "
                   "That should just about do it, but I reserve the right to add more later.")
        cta-copy "[Lorem Ipsum] CAT CTA"
        cta-target "/_components"]
    (when id
      [:div.bg-pale-purple
       [:div.split-organism.max-1080.mx-auto
        [:div.split-left-on-tb-dt.split-top-on-mb.mx-auto
         (ui/img {:src img-url
                  :style {:object-fit "cover"}
                  :width "100%"
                  :height "100%"
                  :alt img-alt
                  :class "self-end"})]
        [:div.my-auto.mx-auto.split-right-on-tb-dt.split-bottom-on-mb.p4
         [:h2.title-1.canela primary]
         [:div.mt4
          [:div secondary]
          [:div.shout.col-8.mx-auto.pt3 (ui/button-medium-primary
                                         {:href      cta-target
                                          :data-test (str "go-to-" id)}
                                         cta-copy)]]]]])))
