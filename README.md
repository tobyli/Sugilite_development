# Sugilite
## Abstract

SUGILITE is a new programming-by-demonstration (PBD) system that enables users to create automation on smartphones. SUGILITE uses Android’s accessibility API to support automating arbitrary tasks in any Android app (or even across multiple apps). When the user gives verbal commands that SUGILITE does not know how to execute, the user can demonstrate by directly manipulating the regular apps’ user interface. By leveraging the verbal instructions, the demonstrated procedures, and the apps’ UI hierarchy structures, SUGILITE can automatically generalize the script from the recorded actions, so SUGILITE learns how to perform tasks with different variations and parameters from a single demonstration. Extensive error handling and context checking support forking the script when new situations are encountered, and provide robustness if the apps change their user interface. Our lab study suggests that users with little or no programming knowledge can successfully automate smartphone tasks using SUGILITE.

## Installation:

1. Build and Install the app

2. Grant the storage access and network permissions (Go to Phone Settings -> Apps -> Sugilite -> Permissions)

3. (For phones with Android 6.0+ (API >= 23)) Grant the overlay permission (Go to Phone Settings -> Apps -> Settings icon on the upper right corner -> Draw over other apps -> Sugilite

4. Enable the accessibility service (Go to Phone Settings -> Accessibility -> Sugilite)

5. Make sure that you can see a duck icon on the screen

6. Set the address of the semantic parsing server  (Launch Sugilite -> open the top right menu -> Settings -> General -> Semantic Parsing Server Address) (~~a demo parsing server is running at http://35.211.149.88:4567/semparse~~)


## Reference:
Toby Jia-Jun Li, Amos Azaria, and Brad A. Myers. [SUGILITE: Creating Multimodal Smartphone Automation by Demonstration.](http://toby.li/files/TobyLi-CHI2017-Sugilite.pdf) Proceedings of the 2017 CHI Conference on Human Factors in Computing Systems  (CHI 2017)

Toby Jia-Jun Li, Yuanchun Li, Fanglin Chen, and Brad A. Myers. [Programming IoT Devices by Demonstration Using Mobile Apps.](http://toby.li/files/TobyLi-ISEUD2017-ProgrammingIoTDevicesByDemonstration.pdf) Proceedings of the International Symposium on End User Development (IS-EUD 2017).

Toby Jia-Jun Li, Igor Labutov, Xiaohan Nancy Li, Xiaoyi Zhang, Wenze Shi, Wanling Ding, Tom M. Mitchell, and Brad A. Myers. [APPINITE: A Multi-Modal Interface for Specifying Data Descriptions in Programming by Demonstration Using Natural Language Instructions.](http://toby.li/files/TobyLi-VLHCC18-APPINITE.pdf) Proceedings of  the 2018 IEEE Symposium on Visual Languages and Human-Centric Computing (VL/HCC 2018).

Toby Jia-Jun Li, Marissa Radensky, Justin Jia, Kirielle Singarajah, Tom M. Mitchell, and Brad A. Myers. [PUMICE: A Multi-Modal Agent that Learns Concepts and Conditionals from Natural Language and Demonstrations.](http://toby.li/files/Li_Pumice_UIST19.pdf) Proceedings of the 32nd Annual ACM Symposium on User Interface Software and Technology (UIST 2019).

Toby Jia-Jun Li, Tom Mitchell, and Brad A. Myers. [Interactive Task Learning from GUI-Grounded Natural Language Instructions and Demonstrations.](http://toby.li/files/li-interactive-acl2020.pdf) Proceedings of the 58th Annual Meeting of the Association for Computational Linguistics (ACL 2020): System Demonstrations.

Toby Jia-Jun Li, Jingya Chen, Brandon Canfield, and Brad A. Myers. [Privacy-Preserving Script Sharing in GUI-based Programming-by-Demonstration Systems.](http://toby.li/files/li-privacy-cscw2020.pdf) Proceedings of the ACM on Human-Computer Interaction (CSCW 2020).

Toby Jia-Jun Li, Jingya Chen, Haijun Xia, Tom M. Mitchell, and Brad A. Myers. [Multi-Modal Repairs of Conversational Breakdowns in Task-Oriented Dialogs.](https://toby.li/files/Li_Sovite_Error_Handling_UIST2020.pdf) Proceedings of the ACM Symposium on User Interface Software and Technology (UIST 2020).


