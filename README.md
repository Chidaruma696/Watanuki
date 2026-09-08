[🇬🇧 English](README.en.md)

<div align="center">
  <br/>
  <img src="docs/icon.png" width="180" alt="Icono de Watanuki" />

# Watanuki

**渡 · Un visor de anime de código abierto para Latinoamérica, con estética manga.**

<br/>

![Android 8.0+](https://img.shields.io/badge/android-8.0%2B-3ddc84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/kotlin-2.2-7f52ff?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/jetpack%20compose-ui-4285f4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![libVLC](https://img.shields.io/badge/libVLC-3.6-ff8800?style=for-the-badge&logo=vlcmediaplayer&logoColor=white)
![Licencia Apache 2.0](https://img.shields.io/badge/licencia-Apache%202.0-1b150d?style=for-the-badge)

<br/>

[![Descargar APK](https://img.shields.io/github/v/release/Chidaruma696/Watanuki?label=%F0%9F%93%B2%20DESCARGAR%20APK&style=for-the-badge&color=2b2140)](https://github.com/Chidaruma696/Watanuki/releases/latest)

<br/>

*59 fuentes en español compiladas dentro de la app · sin extensiones · sin anuncios · sin rastreo*

</div>

---

> [!IMPORTANT]
> **Watanuki no aloja, sube ni distribuye ningún vídeo.** Solo lee sitios públicos de terceros, igual que un navegador.
> Si puedes pagar una plataforma legal, hazlo. Este proyecto existe por una carencia, no como sustituto. Lee [por qué existe](#-por-qué-existe-watanuki) y [cómo apoyar al anime](#-apoya-al-anime-de-verdad).

<br/>

## 📲 Descargar

1. Entra en la [última versión](https://github.com/Chidaruma696/Watanuki/releases/latest) y baja el archivo `Watanuki-x.y.z.apk`.
2. Ábrelo en el móvil. Android te pedirá permiso para instalar apps de esta fuente; acéptalo una vez.
3. Listo. Las siguientes versiones se instalan encima sin perder ajustes ni descargas.

Watanuki no está en Play Store ni va a estarlo; se distribuye solo desde aquí. Y mientras Android siga siendo abierto, con eso basta ([por qué importa](#-keep-android-open)).

<br/>

## 🗺️ Qué es

Watanuki es una app Android para ver anime desde sitios en español, pensada para gente que vive en un país donde la oferta legal llega tarde, incompleta o directamente no llega. Toma las fuentes de la comunidad de [Aniyomi](https://github.com/Kohi-den/extensions-source), las **compila dentro del APK** (nada de instalar extensiones sueltas), las envuelve en una interfaz de estética manga y les pone un reproductor serio.

| 📺 Ver | 📥 Guardar | 🎨 Vivir |
| --- | --- | --- |
| Feed de inicio con recomendaciones, últimos episodios y populares por fuente | Descargas en paralelo, por episodio o temporada completa, con reanudación | Diez paletas inspiradas en personajes de Touhou, en claro y oscuro |
| Reproductor libVLC: cualquier códec, seis modos de escala, saltos rápidos | Cola con progreso, reintentos y verificación de espacio | Sistema de diseño "Manga" portado de Komi Store: papel, tinta, sellos y tramas |
| Elección automática del mejor servidor o lista manual | Reproducción sin conexión de lo guardado | Filtro de contenido adulto **desactivado por defecto** desde el primer arranque |

<br/>

## ✨ Estética Komi

La interfaz de Watanuki es una adaptación del *personality* **Manga** de [Komi Store](https://github.com/komi-store/komi-store), un sistema de diseño que renuncia a Material para parecerse a una página de manga:

- 📄 **Papel y tinta**: fondo crema de día, negro tinta de noche; nada de grises neutros.
- ▭ **Cero esquinas redondeadas**: paneles con bordes de 3 dp y sombras duras desplazadas, sin desenfoque.
- 🔤 **Anton en mayúsculas** para titulares, Noto Sans para el cuerpo, JetBrains Mono para datos.
- 🩹 **Sellos y tramas**: cabeceras con marcador inclinado, tramas de puntos en las esquinas, botones que se "estampan" al pulsar.
- 🎌 *Kickers* en japonés en cada sección (今日 · PARA TI, 保存 · DESCARGAS).

Los colores no son los de Komi: cada tema toma su paleta de un personaje de Touhou Project (Reimu, Marisa, Patchouli, Sakuya, Remilia, Flandre, Cirno, Youmu, Yuyuko, Alice), cada uno con versión de día y de noche.

<br/>

## 💔 Por qué existe Watanuki

Esto no es una app "para no pagar". Es una respuesta a un problema concreto y documentado: **en Latinoamérica el anime legal es escaso, fragmentado y caro en proporción al ingreso.**

- 📉 **La oferta legal no cubre lo que se estrena.** Cada temporada hay series relevantes que ninguna plataforma licencia para la región, o que llegan con semanas o meses de retraso respecto a Japón. Cuando no hay derechos de simulcast, un territorio puede esperar seis semanas, tres meses o más de un año por acceso legal, si es que llega ([CBR, otoño 2025](https://www.cbr.com/fall-2025-anime-streaming-limbo/); [Vitrina](https://vitrina.ai/blog/anime-regional-licensing-restrictions/)).
- 🧩 **Los catálogos se rompen por país.** Un anime puede estar en Crunchyroll en Estados Unidos y no en México porque otra empresa tiene la exclusiva regional; las licencias caducan y las series desaparecen sin aviso ([Level Up](https://www.levelup.com/noticias/crunchyroll-elimina-sin-avisar-mas-de-5-animes-muy-queridos-de-su-catalogo-y-demuestra-los-peligros-del-formato-digital/)). HiDive se fue de Latinoamérica en 2024 y dejó títulos sin ninguna vía legal ([MyAnimeList](https://myanimelist.net/stacks/58905)).
- 🌎 **La región ya está viendo anime; solo que sin opción legal.** Brasil es el segundo país del mundo en piratería de anime; México, Colombia, Chile y Argentina forman el grueso de la audiencia del mayor sitio pirata de anime del planeta ([CBR](https://www.cbr.com/hianime-biggest-piracy-streaming-america-government-threat/); [Advanced Television](https://advanced-television.com/2016/10/17/anime-hit-by-7-7bn-pirate-visits/)). Según MUSO, en 2024 México sumó 4.400 millones de visitas a sitios piratas y Brasil 4.100 millones ([MUSO 2024](https://www.muso.com/hubfs/MUSO%202024%20Piracy%20Trends%20and%20Insights.pdf)).
- ☠️ **Y lo hace en sitios peligrosos.** Las webs piratas de anime en Brasil resultaron hasta 80 veces más arriesgadas que un sitio legítimo en cuanto a malware y fraude ([Advanced Television, 2026](https://www.advanced-television.com/2026/05/01/studies-highlight-latam-piracy-cybersecurity-risks/)). Watanuki al menos elimina esa capa: sin anuncios, sin rastreadores, sin pop-ups, sin instalar nada raro.

Watanuki no arregla nada de eso. Solo hace que, mientras la industria no llega, el camino que la gente ya usa sea más limpio y más seguro.

<br/>

## 🙇 A los autores, estudios y animadores

Pedimos disculpas. De verdad, no como fórmula.

Detrás de cada episodio hay personas que trabajan en condiciones que la propia industria reconoce como insostenibles: animadores jóvenes con sueldos anuales por debajo de los dos millones de yenes (unos 13.000 dólares), jornadas que superan las doce horas, y entre el 50 % y el 70 % del gremio trabajando como autónomos sin protección laboral ([Infobae](https://www.infobae.com/america/mundo/2025/02/17/japon-enfrenta-fuertes-denuncias-laborales-en-la-industria-del-anime/); [Nippon.com](https://www.nippon.com/es/in-depth/d01174/); [Somos Kudasai](https://somoskudasai.com/noticias/animador-japones-revela-salario-explotacion-industria-anime-pirateria/)).

Cada vista que no pasa por un canal oficial es una vista que no vuelve a ellos. Somos conscientes. Por eso este proyecto no tiene publicidad, no gana dinero y no pretende competir con nadie: si mañana toda la oferta llegara legalmente a la región, Watanuki debería desaparecer, y nos parecería bien.

<br/>

## 💚 Apoya al anime de verdad

Si usas Watanuki, **devuelve algo**. No hace falta mucho; hace falta que sea real. En orden de impacto:

1. 💳 **Paga una plataforma legal** aunque no tenga todo. Crunchyroll, Netflix, Prime Video o Pluto TV licencian para la región y esas cifras de audiencia son lo que hace que un estudio vea Latinoamérica como mercado y no como pérdida.
2. 📀 **Compra Blu-ray, manga y bandas sonoras oficiales** cuando lleguen a tu país. Es el ingreso más directo para los comités de producción.
3. 🧸 **Merchandising oficial, no copias.** Una figura o un artbook con licencia paga sueldos; la imitación paga a otro.
4. 🎟️ **Ve al cine** cuando estrenen una película de anime en tu ciudad. Los distribuidores locales deciden qué traer según lo que llenó salas la última vez.
5. 📣 **Pide lo que quieres ver, por los canales oficiales.** Las plataformas leen las peticiones por región; el catálogo de tu país se negocia con esos datos.
6. 💬 **Habla del anime, no del sitio pirata.** Recomienda la obra, el estudio, la autora. Que crezca la demanda de lo legal, no del atajo.

Y si un anime que ves en Watanuki aparece en una plataforma legal en tu país, **cambia**. Es el trato.

<br/>

## 🔧 Cómo funciona

```
Watanuki
├── app/              Interfaz Compose (feed, catálogo, ficha, descargas, ajustes), reproductor libVLC, proxy de streaming
└── anime-sources/    Fuentes de Aniyomi compiladas como módulo + runtime de la API extensions-lib 14
    ├── upstream/     Copia de Kohi-den/extensions-source (src/es, lib, lib-multisrc), Apache 2.0
    └── src/          Runtime: red, cookies, Cloudflare, motor JavaScript, modelos, registro generado
```

- 🧩 **Fuentes compiladas, no extensiones.** Un script de Gradle recorre `upstream/src/es`, añade cada fuente y cada extractor como *source set* del módulo y genera un registro con las clases. No hay APKs que instalar ni permisos para verlas.
- 🌐 **Runtime propio de la API de Aniyomi.** Las fuentes están escritas contra `extensions-lib` v14; Watanuki implementa esa API (`AnimeHttpSource`, modelos, red) con código de Aniyomi y el nuestro.
- 🎬 **Proxy local para el reproductor.** libVLC solo acepta User-Agent y Referer, así que los vídeos pasan por un servidor HTTP en `127.0.0.1` que reenvía con todas las cabeceras y cookies de la fuente y reescribe las listas HLS.
- 📦 **Descargas propias.** OkHttp con reanudación por rangos, HLS concatenado a un solo archivo, cola con varios trabajadores y verificación de espacio.

### Compilar

```bash
git clone https://github.com/Chidaruma696/Watanuki.git
cd Watanuki
./gradlew :app:assembleDebug        # APK en app/build/outputs/apk/debug/
```

Requisitos: JDK 17 o superior y el SDK de Android 36. Para actualizar las fuentes al último estado de la comunidad: `tools/update-sources.sh`.

<br/>

## 🗺️ Hoja de ruta

- [ ] Biblioteca e historial con progreso por episodio
- [ ] Gestos en el reproductor: pinch para zoom, brillo y volumen, PiP
- [ ] Subtítulos y pistas de audio
- [ ] Descarga de streams HLS cifrados
- [ ] Más idiomas de fuentes (hoy solo `es`)
- [ ] Sincronización con AniList y MyAnimeList

<br/>

## ⚖️ Legal

Watanuki se distribuye bajo la [licencia Apache 2.0](LICENSE). No contiene ni distribuye contenido con derechos de autor; accede a sitios de terceros que el usuario elige. Los nombres de los temas son personajes de Touhou Project, propiedad de Team Shanghai Alice, sin afiliación alguna. Todo el código y las bibliotecas de terceros, con sus licencias, están en [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Watanuki usa **libVLC** (VideoLAN) bajo LGPL 2.1, sin modificar; su código fuente está en [code.videolan.org](https://code.videolan.org/videolan/vlc-android).

<br/>

## 📢 Keep Android Open

> **Tu teléfono está a punto de dejar de ser tuyo.** [keepandroidopen.org/es](https://keepandroidopen.org/es/)

Google anunció en 2025 una **verificación obligatoria de desarrolladores**, en vigor a partir de 2027: quien publique una app para Android tendrá que registrarse en un sistema central de Google, pagar una cuota y entregar su documento de identidad. Las apps de quien no se registre **quedarán bloqueadas en todos los dispositivos certificados del mundo**, estén o no en Play Store, incluidas las de F-Droid. Instalar por tu cuenta pasará a ser un proceso de nueve pasos con 24 horas de espera, controlado por Google Play Services y revocable en cualquier momento.

Que quede claro: **si eso se aplica, Watanuki deja de existir.** Y Yuko, y cualquier app libre que no pase por la caja de Google. Este proyecto solo es posible porque Android todavía es abierto.

Por eso la app muestra un aviso en Inicio (se puede ocultar) y un enlace permanente en Ajustes › Acerca de. La campaña la impulsan 71 organizaciones de 23 países (F-Droid, EFF, FSF, Nextcloud, Proton, KDE, Tor Project, LineageOS, GNOME, Brave…). Lo que pide es sencillo:

- 📲 **Instala F-Droid** en cada dispositivo Android que tengas.
- ✍️ **Firma la petición** y comparte la página.
- 🏛️ **Escribe a tu regulador** de competencia o protección al consumidor.
- 🧑‍💻 Si desarrollas: **no te registres**, y convence a otros de no hacerlo.

<br/>

<div align="center">

渡 · わたぬき

</div>
