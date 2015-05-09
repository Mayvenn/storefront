(ns storefront.components.tos
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]))

(defn tos-component [data owner]
  (om/component
   (html
    [:div
     [:p {:align "center"} [:strong "Mayvenn"]]
     " "
     [:p {:align "center"} [:strong "TERMS OF SERVICE"]]
     " "
     [:p " " [:strong "Date of Last Revision: November, 2014"] " "]
     " "
     [:p " " [:strong] " "]
     " "
     [:p " " [:strong "Welcome to Mayvenn!"] " "]
     " "
     [:p
      " Mayvenn, Inc. (“Company,” “we,” “us,” “our”) provides its services (described below) to you through its website located at www.mayvenn.com (the “Site”) and through its mobile applications and related services (collectively, such services, including any new features and applications, and the Site, the “Service(s)”), subject to the following Terms of Service (as amended from time to time, the “Terms of Service”). We reserve the right, at our sole discretion, to change or modify portions of these Terms of Service at any time. If we do this, we will post the changes on this page and will indicate at the top of this page the date these terms were last revised. We will also notify you, either through the Services user interface, in an email notification or through other reasonable means. Any such changes will become effective no earlier than fourteen (14) days after they are posted, except that changes addressing new functions of the Services or changes made for legal reasons will be effective immediately. Your continued use of the Service after the date any such changes become effective constitutes your acceptance of the new Terms of Service. "]
     " "
     [:p
      " In addition, when using certain services, you will be subject to any additional terms applicable to such services that may be posted on the Service from time to time, including, without limitation, the Privacy Policy located at "
      [:a
       {:href "https://shop.mayvenn.com/policy/privacy"}
       "https://shop.mayvenn.com/policy/privacy"]
      ". All such terms are hereby incorporated by reference into these Terms of Service. "]
     " "
     [:p
      " "
      [:strong [:u "Access and Use of the Service"]]
      " "
      [:strong]
      " "]
     " "
     [:p
      " "
      [:strong "Services Description: "]
      " The Service is designed to allow customer to purchase products online via Mayvenn’s webstore. "]
     " "
     [:p
      " "
      [:strong "Your Registration Obligations: "]
      " You may be required to register with Company in order to access and use certain features of the Service."
      [:strong " "]
      " If you choose to register for the Service, you agree to provide and maintain true, accurate, current and complete information about yourself as prompted by the Service’s registration form. Registration data and certain other information about you are governed by our Privacy Policy. If you are under 13 years of age, you are not authorized to use the Service, with or without registering. In addition, if you are under 18 years old, you may use the Service, with or without registering, only with the approval of your parent or guardian. "]
     " "
     [:p
      " "
      [:strong "Member Account, Password and Security: "]
      " You are responsible for maintaining the confidentiality of your password and account, if any, and are fully responsible for any and all activities that occur under your password or account. You agree to (a) immediately notify Company of any unauthorized use of your password or account or any other breach of security, and (b) ensure that you exit from your account at the end of each session when accessing the Service. Company will not be liable for any loss or damage arising from your failure to comply with this Section. "]
     " "
     [:p " " [:strong] " "]
     " "
     [:p
      " "
      [:strong " Modifications to Service: "]
      " Company reserves the right to modify or discontinue, temporarily or permanently, the Service (or any part thereof) with or without notice. You agree that Company will not be liable to you or to any third party for any modification, suspension or discontinuance of the Service. "]
     " "
     [:p " " [:strong] " "]
     " "
     [:p
      " "
      [:strong " "]
      " "
      [:strong "General Practices Regarding Use and Storage"]
      " : You acknowledge that Company may establish general practices and limits concerning use of the Service, including without limitation the maximum period of time that data or other content will be retained by the Service and the maximum storage space that will be allotted on Company’s servers on your behalf. You agree that Company has no responsibility or liability for the deletion or failure to store any data or other content maintained or uploaded by the Service. You acknowledge that Company reserves the right to terminate accounts that are inactive for an extended period of time. You further acknowledge that Company reserves the right to change these general practices and limits at any time, in its sole discretion, with or without notice. "]
     " "
     [:p
      " "
      [:strong "Mobile Services: "]
      " The Service includes certain services that are available via a mobile device, including (i) the ability to upload content to the Service via a mobile device, (ii) the ability to browse the Service and the Site from a mobile device and (iii) the ability to access certain features through an application downloaded and installed on a mobile device (collectively, the “Mobile Services”). To the extent you access the Service through a mobile device, your wireless service carrier’s standard charges, data rates and other fees may apply. In addition, downloading, installing, or using certain Mobile Services may be prohibited or restricted by your carrier, and not all Mobile Services may work with all carriers or devices. By using the Mobile Services, you agree that we may communicate with you regarding Company and other entities by SMS, MMS, text message or other electronic means to your mobile device and that certain information about your usage of the Mobile Services may be communicated to us. In the event you change or deactivate your mobile telephone number, you agree to promptly update your Company account information to ensure that your messages are not sent to the person that acquires your old number. "
      [:strong [:em]]
      " "]
     " "
     [:p " " [:strong] " "]
     " "
     [:p " " [:strong [:u "Conditions of Use"]] " "]
     " "
     [:p " " [:strong [:u]] " "]
     " "
     [:p
      " "
      [:strong " User Conduct: "]
      " You are solely responsible for all code, video, images, information, data, text, software, music, sound, photographs, graphics, messages or other materials (“content”) that you upload, post, publish or display (hereinafter, “upload”) or email or otherwise use via the Service. The following are examples of the kind of content and/or use that is illegal or prohibited by Company. Company reserves the right to investigate and take appropriate legal action against anyone who, in Company’s sole discretion, violates this provision, including without limitation, removing the offending content from the Service, suspending or terminating the account of such violators and reporting you to the law enforcement authorities. You agree to not use the Service to: "]
     " "
     [:p
      " a) email or otherwise upload any content that (i) infringes any intellectual property or other proprietary rights of any party; (ii) you do not have a right to upload under any law or under contractual or fiduciary relationships; (iii) contains software viruses or any other computer code, files or programs designed to interrupt, destroy or limit the functionality of any computer software or hardware or telecommunications equipment; (iv) poses or creates a privacy or security risk to any person; (v) constitutes unsolicited or unauthorized advertising, promotional materials, commercial activities and/or sales, “junk mail,” “spam,” “chain letters,” “pyramid schemes,” “contests,” “sweepstakes,” or any other form of solicitation; (vi) is unlawful, harmful, threatening, abusive, harassing, tortious, excessively violent, defamatory, vulgar, obscene, pornographic, libelous, invasive of another’s privacy, hateful racially, ethnically or otherwise objectionable; or (vii) in the sole judgment of Company, is objectionable or which restricts or inhibits any other person from using or enjoying the Service, or which may expose Company or its users to any harm or liability of any type; "]
     " "
     [:p
      " b) interfere with or disrupt the Service or servers or networks connected to the Service, or disobey any requirements, procedures, policies or regulations of networks connected to the Service; or "]
     " "
     [:p
      " c) violate any applicable local, state, national or international law, or any regulations having the force of law; "]
     " "
     [:p
      " d) impersonate any person or entity, or falsely state or otherwise misrepresent your affiliation with a person or entity; "]
     " "
     [:p
      " e) solicit personal information from anyone under the age of 18; "]
     " "
     [:p
      " f) harvest or collect email addresses or other contact information of other users from the Service by electronic or other means for the purposes of sending unsolicited emails or other unsolicited communications; "]
     " "
     [:p
      " g) advertise or offer to sell or buy any goods or services for any business purpose that is not specifically authorized; "]
     " "
     [:p
      " h) further or promote any criminal activity or enterprise or provide instructional information about illegal activities; or "]
     " "
     [:p
      " i) obtain or attempt to access or otherwise obtain any materials or information through any means not intentionally made available or provided for through the Service. "]
     " "
     [:p
      " "
      [:strong
       " Special Notice for International Use; Export Controls: "]
      " Software (defined below) available in connection with the Service and the transmission of applicable data, if any, is subject to United States export controls. No Software may be downloaded from the Service or otherwise exported or re-exported in violation of U.S. export laws. Downloading or using the Software is at your sole risk. Recognizing the global nature of the Internet, you agree to comply with all local rules and laws regarding your use of the Service, including as it concerns online conduct and acceptable content. "]
     " "
     [:p
      " "
      [:strong " Commercial Use: "]
      " Unless otherwise expressly authorized herein or in the Service, you agree not to display, distribute, license, perform, publish, reproduce, duplicate, copy, create derivative works from, modify, sell, resell, exploit, transfer or upload for any commercial purposes, any portion of the Service, use of the Service, or access to the Service. The Service is for your personal use. "]
     " "
     [:p " " [:strong [:u "Intellectual Property Rights"]] " "]
     " "
     [:p " " [:strong [:u]] " "]
     " "
     [:p
      " "
      [:strong " Service Content, Software and Trademarks: "]
      " You acknowledge and agree that the Service may contain content or features (“Service Content”) that are protected by copyright, patent, trademark, trade secret or other proprietary rights and laws. Except as expressly authorized by Company, you agree not to modify, copy, frame, scrape, rent, lease, loan, sell, distribute or create derivative works based on the Service or the Service Content, in whole or in part, except that the foregoing does not apply to your own User Content (as defined below) that you legally upload to the Service. In connection with your use of the Service you will not engage in or use any data mining, robots, scraping or similar data gathering or extraction methods. Any use of the Service or the Service Content other than as specifically authorized herein is strictly prohibited. The technology and software underlying the Service or distributed in connection therewith is the property of Company, our affiliates and our partners (the “Software”). You agree not to copy, modify, create a derivative work of, reverse engineer, reverse assemble or otherwise attempt to discover any source code, sell, assign, sublicense, or otherwise transfer any right in the Software. Any rights not expressly granted herein are reserved by Company. "]
     " "
     [:p
      " The Company name and logos are trademarks and service marks of Company (collectively the “Company Trademarks”). Other company, product, and service names and logos used and displayed via the Service may be trademarks or service marks of their respective owners who may or may not endorse or be affiliated with or connected to Company. Nothing in this Terms of Service or the Service should be construed as granting, by implication, estoppel, or otherwise, any license or right to use any of Company Trademarks displayed on the Service, without our prior written permission in each instance. All goodwill generated from the use of Company Trademarks will inure to our exclusive benefit. "]
     " "
     [:p
      " "
      [:strong "Third Party Material: "]
      " Under no circumstances will Company be liable in any way for any content or materials of any third parties (including users), including, but not limited to, for any errors or omissions in any content, or for any loss or damage of any kind incurred as a result of the use of any such content. You acknowledge that Company does not have a duty to pre-screen content, but that Company and its designees will have the right (but not the obligation) in their sole discretion to refuse or remove any content that is available via the Service. Without limiting the foregoing, Company and its designees will have the right to remove any content that violates these Terms of Service or is deemed by Company, in its sole discretion, to be otherwise objectionable. You agree that you must evaluate, and bear all risks associated with, the use of any content, including any reliance on the accuracy, completeness, or usefulness of such content. "]
     " "
     [:p
      " "
      [:strong "User Content Transmitted Through the Service: "]
      " With respect to the content or other materials you upload through the Service or share with other users or recipients (collectively, “User Content”), you represent and warrant that you own all right, title and interest in and to such User Content, including, without limitation, all copyright and rights of publicity contained therein. By uploading any User Content you hereby grant and will grant Company and its affiliated companies a nonexclusive, worldwide, royalty free, fully paid up, transferable, sublicensable, perpetual, irrevocable license to copy, display, upload, perform, distribute, store, modify and otherwise use your User Content in connection with the operation of the Service or the promotion, advertising or marketing thereof, in any form, medium or technology now known or later developed. "]
     " "
     [:p
      " You acknowledge and agree that any questions, comments, suggestions, ideas, feedback or other information about the Service (“Submissions”), provided by you to Company are non-confidential and Company will be entitled to the unrestricted use and dissemination of these Submissions for any purpose, commercial or otherwise, without acknowledgment or compensation to you. "]
     " "
     [:p
      " You acknowledge and agree that Company may preserve content and may also disclose content if required to do so by law or in the good faith belief that such preservation or disclosure is reasonably necessary to: (a) comply with legal process, applicable laws or government requests; (b) enforce these Terms of Service; (c) respond to claims that any content violates the rights of third parties; or (d) protect the rights, property, or personal safety of Company, its users and the public. You understand that the technical processing and transmission of the Service, including your content, may involve (a) transmissions over various networks; and (b) changes to conform and adapt to technical requirements of connecting networks or devices. "]
     " "
     [:p " " [:strong [:em]] " "]
     " "
     [:p
      " "
      [:strong " Copyright Complaints: "]
      " Company respects the intellectual property of others, and we ask our users to do the same. If you believe that your work has been copied in a way that constitutes copyright infringement, or that your intellectual property rights have been otherwise violated, you should notify Company of your infringement claim in accordance with the procedure set forth below. "]
     " "
     [:p
      " Company will process and investigate notices of alleged infringement and will take appropriate actions under the Digital Millennium Copyright Act (“DMCA”) and other applicable intellectual property laws with respect to any alleged or actual infringement. A notification of claimed copyright infringement should be emailed to Company’s Copyright Agent at help@mayvenn.com (Subject line: “DMCA Takedown Request”). You may also contact us by mail or facsimile at: "]
     " "
     [:p " " [:strong [:em "3060 El Cerrito Plaza"]] " "]
     " "
     [:p " " [:strong [:em "El Cerrito, CA 94530"]] " "]
     " "
     [:p
      " To be effective, the notification must be in writing and contain the following information: "]
     " "
     [:ul
      {:type "disc"}
      " "
      [:li
       " an electronic or physical signature of the person authorized to act on behalf of the owner of the copyright or other intellectual property interest; "]
      " "
      [:li
       " a description of the copyrighted work or other intellectual property that you claim has been infringed; "]
      " "
      [:li
       " a description of where the material that you claim is infringing is located on the Service, with enough detail that we may find it on the Service; "]
      " "
      [:li " your address, telephone number, and email address; "]
      " "
      [:li
       " a statement by you that you have a good faith belief that the disputed use is not authorized by the copyright or intellectual property owner, its agent, or the law; "]
      " "
      [:li
       " a statement by you, made under penalty of perjury, that the above information in your Notice is accurate and that you are the copyright or intellectual property owner or authorized to act on the copyright or intellectual property owner’s behalf. "]
      " "]
     " "
     [:p " " [:strong] " "]
     " "
     [:p
      " "
      [:strong "Counter-Notice"]
      " : If you believe that your User Content that was removed (or to which access was disabled) is not infringing, or that you have the authorization from the copyright owner, the copyright owner’s agent, or pursuant to the law, to upload and use the content in your User Content, you may send a written counter-notice containing the following information to the Copyright Agent: "]
     " "
     [:ul
      {:type "disc"}
      " "
      [:li " your physical or electronic signature; "]
      " "
      [:li
       " identification of the content that has been removed or to which access has been disabled and the location at which the content appeared before it was removed or disabled; "]
      " "
      [:li
       " a statement that you have a good faith belief that the content was removed or disabled as a result of mistake or a misidentification of the content; and "]
      " "
      [:li
       " your name, address, telephone number, and email address, a statement that you consent to the jurisdiction of the federal court located within Northern District of California and a statement that you will accept service of process from the person who provided notification of the alleged infringement. "]
      " "]
     " "
     [:p
      " If a counter-notice is received by the Copyright Agent, Company will send a copy of the counter-notice to the original complaining party informing that person that it may replace the removed content or cease disabling it in 10 business days. Unless the copyright owner files an action seeking a court order against the content provider, member or user, the removed content may be replaced, or access to it restored, in 10 to 14 business days or more after receipt of the counter-notice, at our sole discretion. "]
     " "
     [:p " " [:strong] " "]
     " "
     [:p
      " "
      [:strong " Repeat Infringer Policy"]
      " : In accordance with the DMCA and other applicable law, Company has adopted a policy of terminating, in appropriate circumstances and at Company's sole discretion, users who are deemed to be repeat infringers. Company may also at its sole discretion limit access to the Service and/or terminate the memberships of any users who infringe any intellectual property rights of others, whether or not there is any repeat infringement. "]
     " "
     [:p " " [:strong [:u "Third Party Websites"]] " "]
     " "
     [:p
      " The Service may provide, or third parties may provide, links or other access to other sites and resources on the Internet. Company has no control over such sites and resources and Company is not responsible for and does not endorse such sites and resources. You further acknowledge and agree that Company will not be responsible or liable, directly or indirectly, for any damage or loss caused or alleged to be caused by or in connection with use of or reliance on any content, events, goods or services available on or through any such site or resource. Any dealings you have with third parties found while using the Service are between you and the third party, and you agree that Company is not liable for any loss or claim that you may have against any such third party. "
      [:strong]
      " "]
     " "
     [:p " " [:strong] " "]
     " "
     [:p
      " "
      [:strong [:u "Social Networking Services"]]
      " "
      [:strong [:u]]
      " "]
     " "
     [:p
      " You may enable or log in to the Service via various online third party services, such as social media and social networking services like Facebook or Twitter (“Social Networking Services”). By logging in or directly integrating these Social Networking Services into the Service, we make your online experiences richer and more personalized. To take advantage of this feature and capabilities, we may ask you to authenticate, register for or log into Social Networking Services on the websites of their respective providers. As part of such integration, the Social Networking Services will provide us with access to certain information that you have provided to such Social Networking Services, and we will use, store and disclose such information in accordance with our Privacy Policy. For more information about the implications of activating these Social Networking Services and Company’s use, storage and disclosure of information related to you and your use of such services within Company (including your friend lists and the like), please see our Privacy Policy at "
      [:a
       {:href "https://shop.mayvenn.com/policy/privacy"}
       "https://shop.mayvenn.com/policy/privacy"]
      ". However, please remember that the manner in which Social Networking Services use, store and disclose your information is governed solely by the policies of such third parties, and Company shall have no liability or responsibility for the privacy practices or other actions of any third party site or service that may be enabled within the Service. "]
     " "
     [:p
      " In addition, Company is not responsible for the accuracy, availability or reliability of any information, content, goods, data, opinions, advice or statements made available in connection with Social Networking Services. As such, Company is not liable for any damage or loss caused or alleged to be caused by or in connection with use of or reliance on any such Social Networking Services. Company enables these features merely as a convenience and the integration or inclusion of such features does not imply an endorsement or recommendation. "]
     " "
     [:p " " [:strong [:u "Indemnity and Release"]] " "]
     " "
     [:p
      " You agree to release, indemnify and hold Company and its affiliates and their officers, employees, directors and agent harmless from any from any and all losses, damages, expenses, including reasonable attorneys’ fees, rights, claims, actions of any kind and injury (including death) arising out of or relating to your use of the Service, any User Content, your connection to the Service, your violation of these Terms of Service or your violation of any rights of another. If you are a California resident, you waive California Civil Code Section 1542, which says: “A general release does not extend to claims which the creditor does not know or suspect to exist in his favor at the time of executing the release, which if known by him must have materially affected his settlement with the debtor.” If you are a resident of another jurisdiction, you waive any comparable statute or doctrine. "]
     " "
     [:p " " [:strong [:u "Disclaimer of Warranties"]] " "]
     " "
     [:p
      " YOUR USE OF THE SERVICE IS AT YOUR SOLE RISK. THE SERVICE IS PROVIDED ON AN “AS IS” AND “AS AVAILABLE” BASIS. COMPANY EXPRESSLY DISCLAIMS ALL WARRANTIES OF ANY KIND, WHETHER EXPRESS, IMPLIED OR STATUTORY, INCLUDING, BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, TITLE AND NON-INFRINGEMENT. "]
     " "
     [:p
      " COMPANY MAKES NO WARRANTY THAT (I) THE SERVICE WILL MEET YOUR REQUIREMENTS, (II) THE SERVICE WILL BE UNINTERRUPTED, TIMELY, SECURE, OR ERROR-FREE, (III) THE RESULTS THAT MAY BE OBTAINED FROM THE USE OF THE SERVICE WILL BE ACCURATE OR RELIABLE, OR (IV) THE QUALITY OF ANY PRODUCTS, SERVICES, INFORMATION, OR OTHER MATERIAL PURCHASED OR OBTAINED BY YOU THROUGH THE SERVICE WILL MEET YOUR EXPECTATIONS. "]
     " "
     [:p " " [:strong [:u "Limitation of Liability"]] " "]
     " "
     [:p
      " YOU EXPRESSLY UNDERSTAND AND AGREE THAT COMPANY WILL NOT BE LIABLE FOR ANY INDIRECT, INCIDENTAL, SPECIAL, CONSEQUENTIAL, EXEMPLARY DAMAGES, OR DAMAGES FOR LOSS OF PROFITS INCLUDING BUT NOT LIMITED TO, DAMAGES FOR LOSS OF GOODWILL, USE, DATA OR OTHER INTANGIBLE LOSSES (EVEN IF COMPANY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES), WHETHER BASED ON CONTRACT, TORT, NEGLIGENCE, STRICT LIABILITY OR OTHERWISE, RESULTING FROM: (I) THE USE OR THE INABILITY TO USE THE SERVICE; (II) THE COST OF PROCUREMENT OF SUBSTITUTE GOODS AND SERVICES RESULTING FROM ANY GOODS, DATA, INFORMATION OR SERVICES PURCHASED OR OBTAINED OR MESSAGES RECEIVED OR TRANSACTIONS ENTERED INTO THROUGH OR FROM THE SERVICE; (III) UNAUTHORIZED ACCESS TO OR ALTERATION OF YOUR TRANSMISSIONS OR DATA; (IV) STATEMENTS OR CONDUCT OF ANY THIRD PARTY ON THE SERVICE; OR (V) ANY OTHER MATTER RELATING TO THE SERVICE. IN NO EVENT WILL COMPANY’S TOTAL LIABILITY TO YOU FOR ALL DAMAGES, LOSSES OR CAUSES OF ACTION EXCEED THE AMOUNT YOU HAVE PAID COMPANY IN THE LAST SIX (6) MONTHS, OR, IF GREATER, ONE HUNDRED DOLLARS ($100). "]
     " "
     [:p
      " SOME JURISDICTIONS DO NOT ALLOW THE EXCLUSION OF CERTAIN WARRANTIES OR THE LIMITATION OR EXCLUSION OF LIABILITY FOR INCIDENTAL OR CONSEQUENTIAL DAMAGES. ACCORDINGLY, SOME OF THE ABOVE LIMITATIONS SET FORTH ABOVE MAY NOT APPLY TO YOU. IF YOU ARE DISSATISFIED WITH ANY PORTION OF THE SERVICE OR WITH THESE TERMS OF SERVICE, YOUR SOLE AND EXCLUSIVE REMEDY IS TO DISCONTINUE USE OF THE SERVICE. "]
     " "
     [:p " " [:strong [:u "Arbitration"]] " "]
     " "
     [:p
      " At Company’s or your election, all disputes, claims, or controversies arising out of or relating to the Terms of Service or the Service that are not resolved by mutual agreement may be resolved by binding arbitration to be conducted before JAMS, or its successor. Unless otherwise agreed by the parties, arbitration will be held in Oakland, California before a single arbitrator mutually agreed upon by the parties, or if the parties cannot mutually agree, a single arbitrator appointed by JAMS, and will be conducted in accordance with the rules and regulations promulgated by JAMS unless specifically modified in the Terms of Service. The arbitration must commence within forty-five (45) days of the date on which a written demand for arbitration is filed by either party. The arbitrator’s decision and award will be made and delivered within sixty (60) days of the conclusion of the arbitration and within six (6) months of the selection of the arbitrator. The arbitrator will not have the power to award damages in excess of the limitation on actual compensatory, direct damages set forth in the Terms of Service and may not multiply actual damages or award punitive damages or any other damages that are specifically excluded under the Terms of Service, and each party hereby irrevocably waives any claim to such damages. The arbitrator may, in his or her discretion, assess costs and expenses (including the reasonable legal fees and expenses of the prevailing part) against any party to a proceeding. Any party refusing to comply with an order of the arbitrators will be liable for costs and expenses, including attorneys’ fees, incurred by the other party in enforcing the award. Notwithstanding the foregoing, in the case of temporary or preliminary injunctive relief, any party may proceed in court without prior arbitration for the purpose of avoiding immediate and irreparable harm. The provisions of this arbitration section will be enforceable in any court of competent jurisdiction. "]
     " "
     [:p " " [:strong [:u "Termination"]] " "]
     " "
     [:p
      " You agree that Company, in its sole discretion, may suspend or terminate your account (or any part thereof) or use of the Service and remove and discard any content within the Service, for any reason, including, without limitation, for lack of use or if Company believes that you have violated or acted inconsistently with the letter or spirit of these Terms of Service. Any suspected fraudulent, abusive or illegal activity that may be grounds for termination of your use of Service, may be referred to appropriate law enforcement authorities. Company may also in its sole discretion and at any time discontinue providing the Service, or any part thereof, with or without notice. You agree that any termination of your access to the Service under any provision of this Terms of Service may be effected without prior notice, and acknowledge and agree that Company may immediately deactivate or delete your account and all related information and files in your account and/or bar any further access to such files or the Service. Further, you agree that Company will not be liable to you or any third party for any termination of your access to the Service. "]
     " "
     [:p " " [:strong [:u "User Disputes"]] " "]
     " "
     [:p
      " You agree that you are solely responsible for your interactions with any other user in connection with the Service and Company will have no liability or responsibility with respect thereto. Company reserves the right, but has no obligation, to become involved in any way with disputes between you and any other user of the Service. "]
     " "
     [:p " " [:strong [:u "General"]] " "]
     " "
     [:p
      " These Terms of Service constitute the entire agreement between you and Company and govern your use of the Service, superseding any prior agreements between you and Company with respect to the Service. You also may be subject to additional terms and conditions that may apply when you use affiliate or third-party services, third-party content or third-party software. These Terms of Service will be governed by the laws of the State of California without regard to its conflict of law provisions. With respect to any disputes or claims not subject to arbitration, as set forth above, you and Company agree to submit to the personal and exclusive jurisdiction of the state and federal courts located within Alameda County, California. The failure of Company to exercise or enforce any right or provision of these Terms of Service will not constitute a waiver of such right or provision. If any provision of these Terms of Service is found by a court of competent jurisdiction to be invalid, the parties nevertheless agree that the court should endeavor to give effect to the parties’ intentions as reflected in the provision, and the other provisions of these Terms of Service remain in full force and effect. You agree that regardless of any statute or law to the contrary, any claim or cause of action arising out of or related to use of the Service or these Terms of Service must be filed within one (1) year after such claim or cause of action arose or be forever barred. A printed version of this agreement and of any notice given in electronic form will be admissible in judicial or administrative proceedings based upon or relating to this agreement to the same extent and subject to the same conditions as other business documents and records originally generated and maintained in printed form. You may not assign this Terms of Service without the prior written consent of Company, but Company may assign or transfer this Terms of Service, in whole or in part, without restriction. The section titles in these Terms of Service are for convenience only and have no legal or contractual effect. Notices to you may be made via either email or regular mail. The Service may also provide notices to you of changes to these Terms of Service or other matters by displaying notices or links to notices generally on the Service. "]
     " "
     [:p " " [:strong [:u "Your Privacy"]] " "]
     " "
     [:p
      " At Company, we respect the privacy of our users. For details please see our Privacy Policy. By using the Service, you consent to our collection and use of personal data as outlined therein. "]
     " "
     [:p
      " "
      [:a {:name "OLE_LINK2"}]
      " "
      [:a {:name "OLE_LINK1"} [:strong [:u]]]
      " "]
     " "
     [:p
      " "
      [:strong [:u "Questions? Concerns? Suggestions?"]]
      " "
      [:strong " "]
      " "
      [:br]
      " Please contact us at help@mayvenn.com to report any violations of these Terms of Service or to pose any questions regarding this Terms of Service or the Service. "]
     " "
     [:div " "]])))
