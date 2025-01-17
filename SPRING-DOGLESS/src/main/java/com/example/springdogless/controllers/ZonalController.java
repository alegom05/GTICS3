package com.example.springdogless.controllers;

import com.example.springdogless.DTO.*;
import com.example.springdogless.Repository.*;
import com.example.springdogless.entity.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping({"zonal", "zonal/"})

public class ZonalController {

    @Autowired
    UsuarioRepository usuarioRepository;
    @Autowired
    ReposicionRepository reposicionRepository;
    @Autowired
    ZonalRepository zonaRepository;
    @Autowired
    DistritoRepository distritoRepository;
    @Autowired
    RolRepository rolRepository;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    private ProveedorRepository proveedorRepository;
    @Autowired
    ImportacionRepository importacionRepository;
    @Autowired
    StockProductoRepository stockProductoRepository;

    @GetMapping({""})
    public String PaginaPrincipal(Model model) {
        return "zonal/paginaprincipal";
    }

    @GetMapping("/perfil_zonal")
    public String verperfilzonal(Model model) {
        return "zonal/perfil_zonal"; // Esto renderiza la vista perfil_superadmin.html
    }
    @GetMapping("/cambiarcontraseña")
    public String vercontra(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario != null) {
            model.addAttribute("usuario", usuario); // Pasar el usuario a la vista
        }
        return "zonal/cambiarcontra";
    }

    @GetMapping("/dashboard")
    public String elDashboardEstaTristeYAzul(HttpSession session,Model model) {
        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuario");
        Integer idzona=usuarioLogueado.getZona().getIdzonas();
        //Top 10 productos más importados en su zona.
        List<ProductoImportadoDTO> productosTop10 = importacionRepository.topProductosImportados(idzona);
        model.addAttribute("productosTop10", productosTop10);


        /*Top 10 de usuarios finales con más importaciones.
        -

        -
        Productos con poco stock para un reposición.
        -
        Tabla o vista de los 3 agentes a su cargo.*/

        //Cantidad de usuarios registrados vs activos en la zona.
        Integer usuariosRegistrados=usuarioRepository.usuariosRegistradosPorZona(idzona);
        model.addAttribute("usuariosRegistrados", usuariosRegistrados);
        Integer usuariosActivos=usuarioRepository.usuariosActivosPorZona(idzona);
        model.addAttribute("usuariosActivos", usuariosActivos);

        //Cantidad de usuarios asignados por cada agente, tambien hallaremos sus datos aqui
        List<AgenteDTO> agentes = usuarioRepository.findAgentesByJefeId(usuarioLogueado.getId());
        System.out.print("ddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd"+"   "+usuarioLogueado.getId());
        model.addAttribute("agentes", agentes);
        System.out.print("ddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd"+"   "+agentes);

        //top 10 usuarios con mas importaciones

        List<UsuarioCantidadDTO> topcompradores=usuarioRepository.obtenerTotalCantidadPorUsuario();
        List<UsuarioCantidadDTO> top10compradores =topcompradores.stream().limit(10).collect(Collectors.toList());

        model.addAttribute("usuariosTop10", top10compradores);


        // Obtener la lista completa de productos ordenados por menor stock
        List<ProductoStockDTO> productosConMenorStock = stockProductoRepository.findProductosConMenorStock();
        List<ProductoStockDTO> productosLimitados = productosConMenorStock.stream()
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("productosConMenorStock", productosLimitados);

        return "zonal/dashboard";
    }

    @GetMapping("/new")
    public String nuevoAgenteFrm(Model model) {
        model.addAttribute("listaZonas", zonaRepository.findAll());
        model.addAttribute("listaDistritos", distritoRepository.findAll());
        return "zonal/agregar_agente";
    }


    @GetMapping(value = "/agentes")
    public String listaAgentes(Model model) {
        model.addAttribute("listaAgentes", usuarioRepository.findByRol_RolAndBorrado("Agente",1));
        return "zonal/agentes";
    }

    @GetMapping("/veragente")
    public String verAgente(Model model, @RequestParam("id") int id) {

        Optional<Usuario> optUsuario = usuarioRepository.findById(id);

        if (optUsuario.isPresent()) {
            Usuario usuario = optUsuario.get();
            model.addAttribute("usuario", usuario);
            return "zonal/verAgente";
        } else {
            return "redirect:/zonal/agentes";
        }
    }

    @GetMapping("/nuevoAgente")
    public String nuevoAgente(Model model, @ModelAttribute("usuario") Usuario usuario) {
        model.addAttribute("listaUsuarios", usuarioRepository.findAll());
        return "zonal/editarAgente";
    }
    @PostMapping("/guardar")
    public String crearAdminZonal(Usuario usuario, @RequestParam("idzonas") Integer idZona,
                                  @RequestParam("iddistritos") Integer idDistrito,
                                  RedirectAttributes attr) {

        // Asignar el rol de Agente (id = 3)
        Rol agenteRole = rolRepository.findById(3)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado"));
        usuario.setRol(agenteRole);

        // Asignar la zona seleccionada
        Zona zona = zonaRepository.findById(idZona)
                .orElseThrow(() -> new IllegalArgumentException("Zona no encontrada"));
        usuario.setZona(zona);

        // Asignar el distrito seleccionado
        Distrito distrito = distritoRepository.findById(idDistrito)
                .orElseThrow(() -> new IllegalArgumentException("Distrito no encontrado"));
        usuario.setDistrito(distrito);
        usuario.setBorrado(1);
        String contrasenaPorDefecto = "contraseñaPredeterminada";
        usuario.setPwd(contrasenaPorDefecto);
        // Guardar el nuevo Adminzonal
        usuarioRepository.save(usuario);
        attr.addFlashAttribute("mensajeExito", "Agente creado correctamente");

        return "redirect:/zonal/agentes";
    }
    @GetMapping("/editagente")
    public String editarAgente(Model model, @RequestParam("id") int id) {

        Optional<Usuario> optUsuario = usuarioRepository.findById(id);

        if (optUsuario.isPresent()) {
            Usuario usuario = optUsuario.get();
            model.addAttribute("usuario", usuario);
            model.addAttribute("listaZonas", zonaRepository.findAll());
            model.addAttribute("listaDistritos", distritoRepository.findAll());
            return "zonal/editarAgente";
        } else {
            return "redirect:/zonal/agentes";
        }
    }

    @PostMapping("/saveagente")
    public String guardarAgente(@RequestParam("id") int id,
                                @RequestParam("nombre") String nombre,
                                @RequestParam("apellido") String apellido,
                                @RequestParam("email") String email,
                                @RequestParam("telefono") String telefono,
                                @RequestParam("ruc") String ruc,
                                @RequestParam("codigoAduana") String codigoaduana,
                                @RequestParam("razonsocial") String razonsocial,
                                @RequestParam int zona,
                                @RequestParam int distrito,
                                RedirectAttributes attr) {
        // Obtener el usuario existente
        Optional<Usuario> optUsuario = usuarioRepository.findById(id);

        if (optUsuario.isPresent()) {
            Usuario usuario = optUsuario.get();

            // Actualizar solo los campos editables
            usuario.setNombre(nombre);
            usuario.setApellido(apellido);
            usuario.setEmail(email);
            usuario.setTelefono(telefono);
            usuario.setRuc(ruc);
            usuario.setCodigoaduana(codigoaduana);
            usuario.setRazonsocial(razonsocial);

            // Buscar las entidades relacionadas por sus IDs
            Zona zonaEntity = zonaRepository.findById(zona).orElse(null); // Se maneja si no existe
            Distrito distritoEntity = distritoRepository.findById(distrito).orElse(null); // Se maneja si no existe

            // Asignar las entidades encontradas al usuario
            usuario.setZona(zonaEntity);
            usuario.setDistrito(distritoEntity);

            // Guardar el usuario actualizado
            usuarioRepository.save(usuario);
            attr.addFlashAttribute("mensajeExito", "Cambios guardados correctamente");
        } else {
            attr.addFlashAttribute("error", "Usuario no encontrado");
        }

        return "redirect:/zonal/agentes";
    }
    @PostMapping("/deleteagente")
    public String borrarAgente(@RequestParam("id") Integer id, RedirectAttributes attr) {
        Optional<Usuario> optUsuario = usuarioRepository.findById(id);

        if (optUsuario.isPresent()) {
            Usuario usuario = optUsuario.get();
            usuario.setBorrado(0);
            usuarioRepository.save(usuario);
            attr.addFlashAttribute("msg", "Agente borrado exitosamente");
        } else {
            attr.addFlashAttribute("error", "Agente no encontrado");
        }

        return "redirect:/zonal/agentes";
    }

    @PostMapping("/guardarAgente")
    public String guardarAgente(RedirectAttributes attr, Model model,
                                  @ModelAttribute("usuario") @Valid Usuario usuario, BindingResult bindingResult) {
        if (!bindingResult.hasErrors()) { //si no hay errores, se realiza el flujo normal

            if (usuario.getNombre().equals("gaseosa")) {
                model.addAttribute("msg", "Error al crear producto");
                model.addAttribute("listaUsuarios", usuarioRepository.findAll());
                return "zonal/editarZonal";
            } else {
                if (usuario.getId() == 0) {
                    attr.addFlashAttribute("msg", "Usuario creado exitosamente");
                } else {
                    attr.addFlashAttribute("msg", "Usuario actualizado exitosamente");
                }
                usuario.setBorrado(1);
                usuarioRepository.save(usuario);

                return "redirect:/zonal/agentes";
            }

        } else { //hay al menos 1 error
            model.addAttribute("listaUsuarios", usuarioRepository.findAll());
            return "product/editarZonal";
        }
    }

    //Importaciones
    @GetMapping(value = "importaciones")
    public String listaImportaciones(Model model) {
        model.addAttribute("listaImportaciones", importacionRepository.findByBorrado(1));
        return "zonal/importaciones";
    }
    @GetMapping("/editimportaciones")
    public String editarImportaciones(Model model, @RequestParam("id") int id) {

        Optional<Importacion> optImportacion = importacionRepository.findById(id);

        if (optImportacion.isPresent()) {
            Importacion importacion = optImportacion.get();
            model.addAttribute("importacion", importacion);
            model.addAttribute("listaZonas", zonaRepository.findAll());
            model.addAttribute("listaDistritos", distritoRepository.findAll());
            return "zonal/editarImportacion";
        } else {
            return "redirect:/zonal/importaciones";
        }
    }
    @PostMapping("/deleteimportacion")
    public String borrarImportacion(@RequestParam("id") Integer id, RedirectAttributes attr) {
        Optional<Importacion> optImportacion = importacionRepository.findById(id);

        if (optImportacion.isPresent()) {
            Importacion importacion = optImportacion.get();
            importacion.setBorrado(0);
            importacionRepository.save(importacion);
            attr.addFlashAttribute("mensajeExito", "Importacion borrada exitosamente");
        } else {
            attr.addFlashAttribute("error", "Agente no encontrado");
        }

        return "redirect:/zonal/importaciones";
    }
    @PostMapping("/saveimportacion")
    public String guardarImportacion(@RequestParam("id") int id,
                                     @RequestParam("fechaPedido") Date fechaPedido,

                                     RedirectAttributes attr) {
        // Obtener el usuario existente
        Optional<Importacion> optImportacion = importacionRepository.findById(id);

        if (optImportacion.isPresent()) {
            Importacion importacion = optImportacion.get();

            // Actualizar solo los campos editables
            importacion.setFechaPedido(fechaPedido);


            // Guardar el usuario actualizado
            importacionRepository.save(importacion);
            attr.addFlashAttribute("mensajeExito", "Cambios guardados correctamente");
        } else {
            attr.addFlashAttribute("error", "Usuario no encontrado");
        }

        return "redirect:/zonal/importaciones";
    }
    //Puede ser innecesario
    @GetMapping("/verImportacion")
    public String verImportacion(Model model, @RequestParam("id") int id) {

        Optional<Importacion> optionalImportacion = importacionRepository.findById(id);

        if (optionalImportacion.isPresent()) {
            Importacion importacion = optionalImportacion.get();
            model.addAttribute("importacion", importacion);
            return "zonal/verImportacion";
        } else {
            return "redirect:zonal/importaciones";
        }
    }


    //Reposiciones
    @GetMapping(value = "/reposiciones")
    public String listaReposiciones(Model model) {
        model.addAttribute("listaReposiciones", reposicionRepository.findByBorrado(1));
        return "zonal/reposiciones";
    }

    @GetMapping("/verReposicion")
    public String verReposicion(Model model, @RequestParam("id") int id) {

        Optional<Reposicion> optReposicion = reposicionRepository.findById(id);

        if (optReposicion.isPresent()) {
            Reposicion reposicion = optReposicion.get();
            model.addAttribute("reposicion", reposicion);
            return "zonal/verReposicion";
        } else {
            return "redirect:zonal/reposiciones";
        }
    }

    @GetMapping("/nuevaReposicion")
    public String nuevaReposicion(Model model, @ModelAttribute("reposicion") Reposicion reposicion) {
        model.addAttribute("listaReposiciones", reposicionRepository.findAll());
        List<Proveedor> listaProveedores = proveedorRepository.findByNombreIsNotNullAndApellidoIsNotNull();
        model.addAttribute("listaProveedores", listaProveedores);
        model.addAttribute("listaProductos", productRepository.findAll());
        // Obtener la lista de zonas
        List<Zona> listaZonas = zonaRepository.findAll();
        model.addAttribute("listaZonas", listaZonas);
        return "zonal/editarReposicion";
    }

    @GetMapping("/editarReposicion")
    public String editarReposicion(@ModelAttribute("reposicion") Reposicion reposicion,
                                   Model model,
                                   @RequestParam(value="id", required = false) int id) {

        Optional<Reposicion> optReposicion = reposicionRepository.findById(id);

        if (optReposicion.isPresent()) {
            reposicion = optReposicion.get();
            model.addAttribute("reposicion", reposicion);
            model.addAttribute("listaReposiciones", reposicionRepository.findAll());
            model.addAttribute("listaProveedores", proveedorRepository.findByNombreIsNotNullAndApellidoIsNotNull());
            model.addAttribute("listaProductos", productRepository.findAll());

            return "zonal/editarReposicionDeVerdad";
        } else {
            return "redirect:/zonal/reposiciones";
        }
    }


    @PostMapping("/guardarReposicion")
    public String guardarReposicion(
            @RequestParam("productoId") Integer productoId,
            @RequestParam("zonaId") String zonaId,
            @RequestParam("fechaPedido") String fechaPedidoStr,
            @RequestParam("cantidad") Integer cantidad,
            @RequestParam("aprobar") String aprobar,
            RedirectAttributes attr,
            Model model) {

        try {
            // Validar cantidad
            if (cantidad <= 0) {
                model.addAttribute("msg", "Error: Cantidad debe ser un número positivo.");
                model.addAttribute("listaProductos", productRepository.findAll());
                return "zonal/editarReposicion";
            }

            if (cantidad > 500) {
                model.addAttribute("msg", "Error: Cantidad no válida.");
                model.addAttribute("listaProductos", productRepository.findAll());
                return "zonal/editarReposicion";
            }

            // Validar fecha de pedido: no debe ser anterior a hoy
            LocalDate fechaPedido = LocalDate.parse(fechaPedidoStr);
            if (fechaPedido.isBefore(LocalDate.now())) {
                model.addAttribute("msg", "Error: Fecha de pedido no puede ser anterior a hoy.");
                model.addAttribute("listaProductos", productRepository.findAll());
                return "zonal/editarReposicion";
            }

            // Buscar Producto
            Producto producto = productRepository.findById(productoId)
                    .orElse(null);
            if (producto == null) {
                model.addAttribute("msg", "Error: Producto no encontrado.");
                model.addAttribute("listaProductos", productRepository.findAll());
                return "zonal/editarReposicion";
            }


            // Buscar Zona
            Zona zona = zonaRepository.findById(Integer.parseInt(zonaId))
                    .orElse(null);
            if (zona == null) {
                model.addAttribute("msg", "Error: Zona no encontrada.");
                model.addAttribute("listaProductos", productRepository.findAll());
                return "zonal/editarReposicion";
            }

            // Crear reposición
            Reposicion reposicion = new Reposicion();
            reposicion.setZona(zona);
            reposicion.setProducto(producto);
            reposicion.setCantidad(cantidad);
            reposicion.setAprobar(aprobar);
            reposicion.setBorrado(1);
            reposicion.setFecha_pedido(Date.valueOf(fechaPedido));

            // Guardar reposición
            reposicionRepository.save(reposicion);

            // Mensaje de éxito
            attr.addFlashAttribute("msg", "Reposición creada exitosamente.");
            return "redirect:/zonal/reposiciones";

        } catch (NumberFormatException e) {
            model.addAttribute("error", "Error al convertir los datos.");
        } catch (Exception e) {
            model.addAttribute("error", "Ocurrió un error al guardar la reposición: " + e.getMessage());
        }

        // Retornar la página con los datos para la vista
        model.addAttribute("listaProductos", productRepository.findAll());
        return "zonal/editarReposicion";
    }



    @PostMapping("/actualizarReposicion")
    public String actualizarReposicion(
            @RequestParam("reposicionId") Integer reposicionId,
            @RequestParam("productoId") Integer productoId,
            @RequestParam("zonaId") String zonaId,
            @RequestParam("fechaPedido") String fechaPedidoStr,
            @RequestParam("cantidad") Integer cantidad,
            RedirectAttributes attr,
            Model model) {

        try {
            // Validación de cantidad
            if (cantidad <= 0) {
                model.addAttribute("msg", "Error: Cantidad debe ser un número positivo.");
                model.addAttribute("listaProductos", productRepository.findAll());
                return "zonal/editarReposicionDeVerdad";
            }

            if (cantidad > 500) {
                model.addAttribute("msg", "Error: Cantidad no válida.");
                model.addAttribute("listaProductos", productRepository.findAll());
                return "zonal/editarReposicionDeVerdad";
            }

            // Validar fecha de pedido
            LocalDate fechaPedido;
            try {
                fechaPedido = LocalDate.parse(fechaPedidoStr);
                if (fechaPedido.isBefore(LocalDate.now())) {
                    model.addAttribute("msg", "Error: Fecha de pedido no puede ser anterior a hoy.");
                    model.addAttribute("listaProductos", productRepository.findAll());
                    return "zonal/editarReposicionDeVerdad";
                }
            } catch (Exception e) {
                model.addAttribute("msg", "Formato de fecha no válido.");
                model.addAttribute("listaProductos", productRepository.findAll());
                return "zonal/editarReposicionDeVerdad";
            }

            // Buscar la reposición por ID
            Reposicion reposicionExistente = reposicionRepository.findById(reposicionId)
                    .orElse(null);

            if (reposicionExistente == null) {
                model.addAttribute("msg", "Error: Reposición no encontrada.");
                model.addAttribute("listaProductos", productRepository.findAll());
                return "zonal/editarReposicionDeVerdad";
            }

            // Buscar el producto
            Producto producto = productRepository.findById(productoId)
                    .orElse(null);

            if (producto == null) {
                model.addAttribute("msg", "Error: Producto no encontrado.");
                model.addAttribute("listaProductos", productRepository.findAll());
                return "zonal/editarReposicionDeVerdad";
            }

            // Validar la zona
            Zona zona;
            try {
                zona = zonaRepository.findById(Integer.parseInt(zonaId)).orElse(null);
                if (zona == null) {
                    model.addAttribute("msg", "Error: Zona no encontrada.");
                    model.addAttribute("listaProductos", productRepository.findAll());
                    return "zonal/editarReposicionDeVerdad";
                }
            } catch (NumberFormatException e) {
                model.addAttribute("msg", "Error: ID de zona no válido.");
                model.addAttribute("listaProductos", productRepository.findAll());
                return "zonal/editarReposicionDeVerdad";
            }

            // Actualizar los campos
            reposicionExistente.setProducto(producto);
            reposicionExistente.setZona(zona);
            reposicionExistente.setCantidad(cantidad);
            reposicionExistente.setFecha_pedido(Date.valueOf(fechaPedido));

            reposicionRepository.save(reposicionExistente);

            // Mensaje de éxito
            attr.addFlashAttribute("msg", "Reposición actualizada exitosamente.");
            return "redirect:/zonal/reposiciones";

        } catch (Exception e) {
            model.addAttribute("error", "Ocurrió un error inesperado: " + e.getMessage());
            model.addAttribute("listaProductos", productRepository.findAll());
            return "zonal/editarReposicionDeVerdad";
        }
    }



    @PostMapping("/borrarReposicion")
    public String borrarAdminZonal(@RequestParam("id") Integer id, RedirectAttributes attr) {
        Optional<Reposicion> optReposicion = reposicionRepository.findById(id);

        if (optReposicion.isPresent()) {
            Reposicion reposicion = optReposicion.get();
            reposicion.setBorrado(2);
            reposicionRepository.save(reposicion);
            attr.addFlashAttribute("msg", "Reposición borrada exitosamente");
        } else {
            attr.addFlashAttribute("error", "Reposición no encontrada");
        }

        return "redirect:/zonal/reposiciones";
    }



    /*
    @GetMapping("/edit")
    public String editarTransportista(@ModelAttribute("product") Product product,
                                      Model model, @RequestParam("id") int id) {

        Optional<Product> optProduct = productRepository.findById(id);

        if (optProduct.isPresent()) {
            product = optProduct.get();
            model.addAttribute("product", product);
            model.addAttribute("listaCategorias", categoryRepository.findAll());
            model.addAttribute("listaProveedores", supplierRepository.findAll());
            return "product/editFrm";
        } else {
            return "redirect:/product";
        }
    }
    */
    @GetMapping("/borrarReposicion")
    public String borrarTransportista(@RequestParam("id") int id,
                                      RedirectAttributes attr) {

        Optional<Reposicion> optReposicion = reposicionRepository.findById(id);

        if (optReposicion.isPresent()) {
            Reposicion reposicion = optReposicion.get();
            reposicion.setBorrado(0);
            attr.addFlashAttribute("msg", "Producto borrado exitosamente");
        } else {
            attr.addFlashAttribute("error", "Producto no encontrado");
        }
        return "redirect:/zonal/reposiciones";

    }

    /*
    @PostMapping("/deleteadminzonal")
    public String borrarAdminZonal(@RequestParam("id") Integer id, RedirectAttributes attr) {
        Optional<Usuario> optUsuario = usuarioRepository.findById(id);

        if (optUsuario.isPresent()) {
            Usuario usuario = optUsuario.get();
            usuario.setBorrado(0);
            usuarioRepository.save(usuario);
            attr.addFlashAttribute("msg", "Admin borrado exitosamente");
        } else {
            attr.addFlashAttribute("error", "Admin no encontrado");
        }

        return "redirect:/admin/adminzonal";
    }
    */
    @PostMapping("/cambiarContrasenia")
    @ResponseBody
    public ResponseEntity<Map<String, String>> cambiarContrasenia(
            @RequestParam("id") int id,
            @RequestParam("oldPassword") String oldPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmNewPassword") String confirmNewPassword) {

        Map<String, String> response = new HashMap<>();
        Optional<Usuario> optUsuario = usuarioRepository.findById(id);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        if (optUsuario.isPresent()) {
            Usuario usuario = optUsuario.get();

            // Verificar si la contraseña antigua es correcta
            if (!passwordEncoder.matches(oldPassword, usuario.getPwd())) {
                response.put("status", "error");
                response.put("message", "La contraseña antigua es incorrecta.");
                return ResponseEntity.ok(response);
            }

            // Verificar si la nueva contraseña y la confirmación coinciden
            if (!newPassword.equals(confirmNewPassword)) {
                response.put("status", "error");
                response.put("message", "La nueva contraseña y su confirmación no coinciden.");
                return ResponseEntity.ok(response);
            }
            // Validar requisitos de la nueva contraseña
            if (newPassword.length() < 8 || newPassword.length() > 16) {
                response.put("status", "error");
                response.put("message", "La contraseña debe tener entre 8 y 16 caracteres.");
                return ResponseEntity.ok(response);
            }

            if (!newPassword.matches("^(?=.*\\d)(?=.*[a-zA-Z])(?=(?:.*[!@#$%^&*]){2}).{8,16}$")) {
                response.put("status", "error");
                response.put("message", "La contraseña debe incluir al menos 1 letra, 1 número y 2 caracteres especiales.");
                return ResponseEntity.ok(response);
            }

            // Cambiar la contraseña si pasa todas las verificaciones
            String newPasswordEncrypted = passwordEncoder.encode(newPassword);
            usuario.setPwd(newPasswordEncrypted);
            usuarioRepository.save(usuario);

            response.put("status", "success");
            response.put("message", "Contraseña cambiada exitosamente.");
            return ResponseEntity.ok(response);
        }

        response.put("status", "error");
        response.put("message", "Usuario no encontrado.");
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<Map<String, String>> cambiarContrasenia(
            @RequestParam("id") int id,
            @RequestParam("oldPassword") String oldPassword,
            @RequestParam("newPassword") String newPassword) {

        Optional<Usuario> optUsuario = usuarioRepository.findById(id);
        Map<String, String> response = new HashMap<>();

        if (optUsuario.isPresent()) {
            Usuario usuario = optUsuario.get();
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

            // Verificar si la contraseña antigua es correcta
            if (!passwordEncoder.matches(oldPassword, usuario.getPwd())) {
                response.put("status", "error");
                response.put("message", "La contraseña antigua es incorrecta.");
                return ResponseEntity.ok(response); // Devuelve error
            }

            // Encriptar la nueva contraseña
            String newPasswordEncrypted = passwordEncoder.encode(newPassword);
            usuario.setPwd(newPasswordEncrypted);
            usuarioRepository.save(usuario);

            response.put("status", "success");
            response.put("message", "Contraseña cambiada exitosamente.");
            return ResponseEntity.ok(response); // Devuelve éxito
        }

        response.put("status", "error");
        response.put("message", "Usuario no encontrado.");
        return ResponseEntity.ok(response); // Devuelve error si no encuentra usuario
    }
    @GetMapping("/informacion-de-contacto")
    public String informaciondecontacto() {
        return "informacion-de-contacto"; // Esto renderiza la vista informacion-de-contacto.html
    }


    @GetMapping("/politica-de-privacidad")
    public String politicadeprivacidad() {
        return "politica-de-privacidad"; // Esto renderiza la vista politica-de-privacidad.html
    }

    @GetMapping("/terminosycondiciones")
    public String terminosycondiciones() {
        return "terminosycondiciones"; // Esto renderiza la vista politica-de-privacidad.html
    }
}
