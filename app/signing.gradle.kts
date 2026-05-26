android {
    signingConfigs {
        create("release") {
            storeFile = file(project.property("keystore.file") as? String ?: "")
            storePassword = project.property("keystore.password") as? String ?: ""
            keyAlias = project.property("keystore.alias") as? String ?: ""
            keyPassword = project.property("keystore.password") as? String ?: ""
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}