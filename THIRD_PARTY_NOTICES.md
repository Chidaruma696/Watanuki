# Avisos de terceros

Watanuki incorpora o depende del siguiente software de terceros. Cada uno conserva su licencia y su copyright.

## Fuentes y API de Aniyomi

| Componente | Uso en Watanuki | Licencia |
| --- | --- | --- |
| [Kohi-den/extensions-source](https://github.com/Kohi-den/extensions-source) | Fuentes en español (`src/es`), extractores de vídeo (`lib`) y plantillas multisitio (`lib-multisrc`), compiladas en `anime-sources/upstream` | Apache 2.0 |
| [aniyomiorg/extensions-lib](https://github.com/aniyomiorg/extensions-lib) (v14) | Modelos e interfaces de la API de fuentes (`SAnime`, `SEpisode`, `Video`, filtros) | Apache 2.0 |
| [aniyomiorg/aniyomi](https://github.com/aniyomiorg/aniyomi) | Capa de red del módulo `core/common`: `NetworkHelper`, cookies, interceptor de Cloudflare, límites de peticiones, `JavaScriptEngine`, DNS sobre HTTPS | Apache 2.0 |

Los archivos de estos proyectos se encuentran en `anime-sources/upstream` (copia íntegra, con su `LICENSE`) y en `anime-sources/src/main/kotlin/eu/kanade/tachiyomi`, donde se indican las adaptaciones.

## Diseño

| Componente | Uso en Watanuki | Licencia |
| --- | --- | --- |
| [Komi Store](https://github.com/komi-store/komi-store) (kurikomi-labs) | Sistema de diseño *personality* "Manga": componentes, decoraciones de tinta, tipografía y forma. Adaptado en `app/src/main/kotlin/com/watanuki/app/ui/komi` | Apache 2.0 |
| [Anton](https://fonts.google.com/specimen/Anton) (Vernon Adams) | Titulares | SIL Open Font License 1.1 |
| [Noto Sans](https://fonts.google.com/noto) (Google) | Cuerpo de texto | SIL Open Font License 1.1 |
| [JetBrains Mono](https://www.jetbrains.com/lp/mono/) (JetBrains) | Texto monoespaciado | SIL Open Font License 1.1 |
| [Material Design Icons](https://fonts.google.com/icons) (Google) | Iconos de las pestañas | Apache 2.0 |

Las paletas de color son propias de Watanuki. Sus nombres corresponden a personajes de Touhou Project, propiedad de Team Shanghai Alice; no existe afiliación ni respaldo.

## Reproductor y descargas

| Componente | Uso en Watanuki | Licencia |
| --- | --- | --- |
| [libVLC](https://code.videolan.org/videolan/vlc-android) (VideoLAN), `org.videolan.android:libvlc-all` | Motor de reproducción, usado sin modificar | LGPL 2.1 |
| [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd) | Proxy HTTP local entre libVLC y los servidores de vídeo | BSD 3-Clause |

Conforme a la LGPL 2.1, libVLC se usa como biblioteca dinámica sin modificaciones. Su código fuente está disponible en el enlace anterior y cualquier usuario puede sustituir la biblioteca por otra versión compatible.

## Bibliotecas

| Componente | Licencia |
| --- | --- |
| [OkHttp](https://github.com/square/okhttp) | Apache 2.0 |
| [Coil](https://github.com/coil-kt/coil) | Apache 2.0 |
| [Jetpack Compose y AndroidX](https://developer.android.com/jetpack) | Apache 2.0 |
| [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines), [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) | Apache 2.0 |
| [RxJava 1](https://github.com/ReactiveX/RxJava) | Apache 2.0 |
| [Injekt](https://github.com/mihonapp/injekt) (fork de Mihon) | Apache 2.0 |
| [QuickJS Android](https://github.com/cashapp/quickjs-java) (Cash App) | Apache 2.0 (motor QuickJS: MIT) |
| [jsunpacker](https://github.com/DatL4g/jsunpacker) | Apache 2.0 |
| [Chicory](https://github.com/dylibso/chicory) | Apache 2.0 |
| [Jsoup](https://jsoup.org/) | MIT |
| [Rhino](https://github.com/mozilla/rhino) | MPL 2.0 |
| [desugar_jdk_libs](https://github.com/google/desugar_jdk_libs) | GPL 2.0 con excepción de classpath |

## Textos de licencia

- Apache License 2.0: <https://www.apache.org/licenses/LICENSE-2.0>
- GNU Lesser General Public License 2.1: <https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html>
- SIL Open Font License 1.1: <https://openfontlicense.org/open-font-license-official-text/>
- MIT License: <https://opensource.org/license/mit>
- BSD 3-Clause License: <https://opensource.org/license/bsd-3-clause>
- Mozilla Public License 2.0: <https://www.mozilla.org/en-US/MPL/2.0/>
