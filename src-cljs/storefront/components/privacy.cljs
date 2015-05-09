(ns storefront.components.privacy
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]))

(defn privacy-component [data owner]
  (om/component
   (html
    [:div
     [:p {:align "center"} " " [:strong [:u "PRIVACY POLICY"]] " "]
     " "
     [:p {:align "center"} " " [:strong [:u " "]] " "]
     " "
     [:p
      {:align "center"}
      " This Privacy Policy was last updated on November, 2014. "]
     " "
     [:p " " [:strong [:u "Our Policy"]] " " [:strong ":"] " "]
     " "
     [:p
      " Welcome to the web site (the “"
      [:strong "Site"]
      "”) of Mayvenn, Inc."
      [:strong [:em " "]]
      "(“"
      [:strong "Company"]
      "”, “we”, “us” and/or “our”). This Site is operated by Company and has been created to provide information about our company and our E-Commerce services and related services (together with the Site, the “"
      [:strong "Services"]
      "”) to our Service visitors (“you”, “your”). This Privacy Policy sets forth Company’s policy with respect to information including personally identifiable data (“"
      [:strong "Personal Data"]
      "”) and other information that is collected from visitors to the Site and Services. "]
     " "
     [:p " " [:strong [:u " "]] " "]
     " "
     [:p
      " "
      [:strong [:u "Information We Collect"]]
      " "
      [:strong ":"]
      " "]
     " "
     [:p
      " When you interact with us through the Services, we may collect Personal Data and other information from you, as further described below:"
      [:u]
      " "]
     " "
     [:p
      " "
      [:strong " Personal Data That You Provide Through the Services: "]
      " We collect Personal Data from you when you voluntarily provide such information, such as when you contact us with inquiries, respond to one of our surveys, register for access to the Services or use certain Services. Wherever Company collects Personal Data we make an effort to provide a link to this Privacy Policy. "]
     " "
     [:p
      " "
      [:strong
       " By voluntarily providing us with Personal Data, you are consenting to our use of it in accordance with this Privacy Policy. If you provide Personal Data to the Services, you acknowledge and agree that such Personal Data may be transferred from your current location to the offices and servers of Company and the authorized third parties referred to herein located in the United States. "]
      " "]
     " "
     [:p " " [:strong " Other Information:"] " "]
     " "
     [:p
      " "
      [:strong [:em "Non-Identifiable Data: "]]
      " When you interact with Company through the Services, we receive and store certain personally non-identifiable information. Such information, which is collected passively using various technologies, cannot presently be used to specifically identify you. Company may store such information itself or such information may be included in databases owned and maintained by Company affiliates, agents or service providers. The Services may use such information and pool it with other information to track, for example, the total number of visitors to our Site, the number of visitors to each page of our Site, and the domain names of our visitors' Internet service providers. It is important to note that no Personal Data is available or used in this process. "]
     " "
     [:p
      " In operating the Services, we may use a technology called \"cookies.\" A cookie is a piece of information that the computer that hosts our Services gives to your browser when you access the Services. Our cookies help provide additional functionality to the Services and help us analyze Services usage more accurately. For instance, our Services may set a cookie on your browser that allows you to access the Services without needing to remember and then enter a password more than once during a visit to the Services. In all cases in which we use cookies, we will not collect Personal Data except with your permission. On most web browsers, you will find a “help” section on the toolbar. Please refer to this section for information on how to receive notification when you are receiving a new cookie and how to turn cookies off. We recommend that you leave cookies turned on because they allow you to take advantage of some of the Service features. "
      [:strong [:em]]
      " "]
     " "
     [:p
      " "
      [:strong [:em "Aggregated Personal Data: "]]
      " In an ongoing effort to better understand and serve the users of the Services, Company often conducts research on its customer demographics, interests and behavior based on the Personal Data and other information provided to us. This research may be compiled and analyzed on an aggregate basis, and Company may share this aggregate data with its affiliates, agents and business partners. This aggregate information does not identify you personally. Company may also disclose aggregated user statistics in order to describe our services to current and prospective business partners, and to other third parties for other lawful purposes. "]
     " "
     [:p " " [:strong " "] " "]
     " "
     [:p
      " "
      [:strong
       [:u "Our Use of Your Personal Data and Other Information"]]
      " "
      [:strong ":"]
      " "]
     " "
     [:p
      " Company uses the Personal Data you provide in a manner that is consistent with this Privacy Policy. If you provide Personal Data for a certain reason, we may use the Personal Data in connection with the reason for which it was provided. For instance, if you contact us by email, we will use the Personal Data you provide to answer your question or resolve your problem. Also, if you provide Personal Data in order to obtain access to the Services, we will use your Personal Data to provide you with access to such services and to monitor your use of such services. Company and its subsidiaries and affiliates (the “Company Related Companies”) may also use your Personal Data and other personally non-identifiable information collected through the Services to help us improve the content and functionality of the Services, to better understand our users and to improve the Services. Company and its affiliates may use this information to contact you in the future to tell you about services we believe will be of interest to you. If we do so, each marketing communication we send you will contain instructions permitting you to \"opt-out\" of receiving future marketing communications. In addition, if at any time you wish not to receive any future marketing communications or you wish to have your name deleted from our mailing lists, please contact us as indicated below. "]
     " "
     [:p
      " If Company intends on using any Personal Data in any manner that is not consistent with this Privacy Policy, you will be informed of such anticipated use prior to or at the time at which the Personal Data is collected. "
      [:strong [:em]]
      " "]
     " "
     [:p
      " "
      [:strong
       [:u "Our Disclosure of Your Personal Data and Other Information"]]
      " "
      [:strong ":"]
      " "]
     " "
     [:p
      " Company is not in the business of selling your information. We consider this information to be a vital part of our relationship with you. There are, however, certain circumstances in which we may share your Personal Data with certain third parties without further notice to you, as set forth below: "]
     " "
     [:p
      " "
      [:strong " Business Transfers:"]
      " As we develop our business, we might sell or buy businesses or assets. In the event of a corporate sale, merger, reorganization, dissolution or similar event, Personal Data may be part of the transferred assets. "]
     " "
     [:p
      " "
      [:strong " Related Companies: "]
      " We may also share your Personal Data with our Related Companies for purposes consistent with this Privacy Policy. "]
     " "
     [:p
      " "
      [:strong "Agents, Consultants and Related Third Parties:"]
      " Company, like many businesses, sometimes hires other companies to perform certain business-related functions. Examples of such functions include mailing information, maintaining databases and processing payments. When we employ another entity to perform a function of this nature, we only provide them with the information that they need to perform their specific function. "]
     " "
     [:p
      " "
      [:strong " Legal Requirements: "]
      " Company may disclose your Personal Data if required to do so by law or in the good faith belief that such action is necessary to (i) comply with a legal obligation, (ii) protect and defend the rights or property of Company, (iii) act in urgent circumstances to protect the personal safety of users of the Services or the public, or (iv) protect against legal liability. "]
     " "
     [:p " " [:strong [:u "Your Choices"]] " " [:strong ":"] " "]
     " "
     [:p
      " You can visit the Site without providing any Personal Data. If you choose not to provide any Personal Data, you may not be able to use certain Services. "]
     " "
     [:p " " [:strong [:u " "]] " "]
     " "
     [:p " " [:strong [:u "Exclusions"]] " " [:strong ":"] " "]
     " "
     [:p
      " This Privacy Policy does not apply to any Personal Data collected by Company other than Personal Data collected through the Services. This Privacy Policy shall not apply to any unsolicited information you provide to Company through the Services or through any other means. This includes, but is not limited to, information posted to any public areas of the Services, such as forums, any ideas for new products or modifications to existing products, and other unsolicited submissions (collectively, “Unsolicited Information”). All Unsolicited Information shall be deemed to be non-confidential and Company shall be free to reproduce, use, disclose, and distribute such Unsolicited Information to others without limitation or attribution."
      [:strong [:u]]
      " "]
     " "
     [:p " " [:strong [:u " "]] " "]
     " "
     [:p " " [:strong [:u "Children"]] " " [:strong ":"] " "]
     " "
     [:p
      " Company does not knowingly collect Personal Data from children under the age of 13. If you are under the age of 13, please do not submit any Personal Data through the Services. We encourage parents and legal guardians to monitor their children’s Internet usage and to help enforce our Privacy Policy by instructing their children never to provide Personal Data on the Services without their permission. If you have reason to believe that a child under the age of 13 has provided Personal Data to Company through the Services, please contact us, and we will endeavor to delete that information from our databases. "
      [:strong [:em]]
      " "]
     " "
     [:h1 " Links to Other Web Sites: "]
     " "
     [:p
      " This Privacy Policy applies only to the Services. The Services may contain links to other web sites not operated or controlled by Company (the “Third Party Sites”). The policies and procedures we described here do not apply to the Third Party Sites. The links from the Services do not imply that Company endorses or has reviewed the Third Party Sites. We suggest contacting those sites directly for information on their privacy policies. "]
     " "
     [:p
      " "
      [:strong [:u "Integrating Social Networking Services"]]
      " : "]
     " "
     [:p
      " One of the special features of the Service is that it allows you to enable or log in to the Services via various social networking services like Facebook or Twitter (“Social Networking Service(s)”). By directly integrating these services, we make your online experiences richer and more personalized. To take advantage of this feature, we will ask you to log into or grant us permission via the relevant Social Networking Service. When you add a Social Networking Services account to the Service or log into the Service using your Social Networking Services account, we will collect relevant information necessary to enable the Service to access that Social Networking Service and your data contained within that Social Networking Service. As part of such integration, the Social Networking Service will provide us with access to certain information that you have provided to the Social Networking Service, and we will use, store and disclose such information in accordance with this Privacy Policy. However, please remember that the manner in which Social Networking Services use, store and disclose your information is governed by the policies of such third parties, and Company shall have no liability or responsibility for the privacy practices or other actions of any Social Networking Services that may be enabled within the Service. "]
     " "
     [:p
      " You may also have the option of posting your Services activities to Social Networking Services when you access content through the Services (for example, you may post to Facebook that you performed an activity on the Service); you acknowledge that if you choose to use this feature, your friends, followers and subscribers on any Social Networking Services you have enabled will be able to view such activity. "]
     " "
     [:h1 " "]
     " "
     [:h1 " Security: "]
     " "
     [:p
      " Company takes reasonable steps to protect the Personal Data provided via the Services from loss, misuse, and unauthorized access, disclosure, alteration, or destruction. However, no Internet or email transmission is ever fully secure or error free. In particular, email sent to or from the Services may not be secure. Therefore, you should take special care in deciding what information you send to us via email. Please keep this in mind when disclosing any Personal Data to Company via the Internet. "]
     " "
     [:p " " [:strong " "] " "]
     " "
     [:h1 " Other Terms and Conditions: "]
     " "
     [:p
      " Your access to and use of the Services is subject to the Terms of Service at "
      [:a
       {:href "https://shop.mayvenn.com/policy/tos"}
       "https://shop.mayvenn.com/policy/tos"]
      [:strong]
      " "]
     " "
     [:h1 " "]
     " "
     [:h1 " Changes to Company’s Privacy Policy: "]
     " "
     [:p
      " The Services and our business may change from time to time. As a result, at times it may be necessary for Company to make changes to this Privacy Policy. Company reserves the right to update or modify this Privacy Policy at any time and from time to time without prior notice. Please review this policy periodically, and especially before you provide any Personal Data. This Privacy Policy was last updated on the date indicated above. Your continued use of the Services after any changes or revisions to this Privacy Policy shall indicate your agreement with the terms of such revised Privacy Policy. "]
     " "
     [:h1 " "]
     " "
     [:h1 " Access to Information; Contacting Company: "]
     " "
     [:p
      " To keep your Personal Data accurate, current, and complete, please contact us as specified below. We will take reasonable steps to update or correct Personal Data in our possession that you have previously submitted via the Services. "]
     " "
     [:p
      " Please also feel free to contact us if you have any questions about Company’s Privacy Policy or the information practices of the Services. "]
     " "
     [:p " You may contact us as follows: "]
     " "
     [:p " Mayvenn, Inc. "]
     " "
     [:p " 3060 El Cerrito Plaza #371 "]
     " "
     [:p " El Cerrito, CA 94530 "]
     " "
     [:div " "]])))
