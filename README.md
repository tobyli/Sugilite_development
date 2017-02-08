# Sugilite
##Abstract

SUGILITE is a new programming-by-demonstration (PBD) system that enables users to create automation on smartphones. SUGILITE uses Android’s accessibility API to support automating arbitrary tasks in any Android app (or even across multiple apps). When the user gives verbal commands that SUGILITE does not know how to execute, the user can demonstrate by directly manipulating the regular apps’ user interface. By leveraging the verbal instructions, the demonstrated procedures, and the apps’ UI hierarchy structures, SUGILITE can automatically generalize the script from the recorded actions, so SUGILITE learns how to perform tasks with different variations and parameters from a single demonstration. Extensive error handling and context checking support forking the script when new situations are encountered, and provide robustness if the apps change their user interface. Our lab study suggests that users with little or no programming knowledge can successfully automate smartphone tasks using SUGILITE.

##Installation:

1. Install the app using the APK (app-release.apk)

2. Grant the storage access permission (Go to Settings -> Apps -> Sugilite -> Permissions)

3. (For phones with Android 6.0+ (API >= 23)) Grant the overlay permission (Go to Settings -> Apps -> Settings icon on the upper right corner -> Draw over other apps -> Sugilite

4. Enable the accessibility service (Go to Settings -> Accessibility -> Sugilite)

## Reference:
Toby Jia-Jun Li, Amos Azaria and Brad A. Myers. 2017. [SUGILITE: Creating Multimodal Smartphone Automation by Demonstration.](http://www.toby.li/sugilite_paper) Proceedings of the 2017 CHI Conference on Human Factors in Computing Systems  (CHI 2017)
