package com.citt.persistence.services;

import com.citt.exceptions.DespachoNotFoundException;
import com.citt.persistence.entity.Despacho;
import com.citt.persistence.repository.DespachoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DespachoServiceImpl implements DespachoService{

    @Autowired
    private DespachoRepository despachoRepository;

    @Override
    public List<Despacho> findAllDespachos() {
        return despachoRepository.findAll();
    }

    @Override
    public Despacho saveDespacho(Despacho despacho) {
        return despachoRepository.save(despacho);
    }

    @Override
    public Despacho updateDespacho(Long idDespacho, Despacho despacho) throws DespachoNotFoundException {
        return despachoRepository.findById(idDespacho).map(existingDespacho -> {

            if (despacho.getFechaDespacho() != null) {
                existingDespacho.setFechaDespacho(despacho.getFechaDespacho());
            }

            if (despacho.getPatenteCamion() != null) {
                existingDespacho.setPatenteCamion(despacho.getPatenteCamion());
            }

            if (despacho.getIntento() != 0) {
                existingDespacho.setIntento(despacho.getIntento());
            }

            if (despacho.getIdCompra() != null) {
                existingDespacho.setIdCompra(despacho.getIdCompra());
            }

            if (despacho.getDireccionCompra() != null) {
                existingDespacho.setDireccionCompra(despacho.getDireccionCompra());
            }

            if (despacho.getValorCompra() != null) {
                existingDespacho.setValorCompra(despacho.getValorCompra());
            }

            existingDespacho.setDespachado(despacho.isDespachado());

            return despachoRepository.save(existingDespacho);

        }).orElseThrow(() ->
            new DespachoNotFoundException("Despacho no encontrado con ID: " + idDespacho)
        );
    }

    @Override
    public void deleteDespacho(Long idDespacho) throws DespachoNotFoundException {
        Optional<Despacho> despacho = despachoRepository.findById(idDespacho);
        if(!despacho.isPresent()){
            throw new DespachoNotFoundException("¡No es posible eliminar! No existe despacho con el ID:" + idDespacho);
        }else {
            despachoRepository.deleteById(idDespacho);
        }
    }

    @Override
    public Despacho findById(Long idDespacho) throws DespachoNotFoundException {
        Optional<Despacho> despacho = despachoRepository.findById(idDespacho);
        if(!despacho.isPresent()) throw new DespachoNotFoundException("¡No existe despacho con el ID:" + idDespacho);
        return despacho.get();
    }
}
