plugins {
  id("signal-library")
  id("kotlin-kapt")
}

android {
  namespace = "org.signal.glide"
}

dependencies {
  implementation(libs.glide.glide)
  kapt(libs.glide.compiler)
}
