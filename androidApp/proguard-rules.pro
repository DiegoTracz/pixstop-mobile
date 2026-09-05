# Regras do R8 para o release.
#
# O que precisa sobreviver ao encolhimento é o que é alcançado por reflexão —
# o R8 não enxerga esses caminhos e removeria as classes por achá-las mortas.

# kotlinx.serialization gera um serializador por classe e o alcança pelo nome.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    static **$* *;
}

# Ktor escolhe o motor HTTP em tempo de execução, via ServiceLoader.
-keep class io.ktor.client.engine.okhttp.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# OkHttp e Okio, que o motor do Ktor usa por baixo.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# Koin resolve dependências por tipo; os construtores precisam continuar lá.
-keep class com.pixstop.mobile.** { *; }

# Coroutines: o campo interno é acessado por reflexão no tratamento de erro.
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
