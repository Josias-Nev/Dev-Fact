package com.devfact.service;

import com.devfact.dao.ClientDao;
import com.devfact.db.DataAccess;
import com.devfact.model.Client;
import com.devfact.util.Texts;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** Logique métier des clients. */
public class ClientService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");

    private final ClientDao clientDao = new ClientDao();

    public List<Client> search(String text, boolean activeOnly) {
        return DataAccess.read(connection -> clientDao.findAll(connection, text, activeOnly));
    }

    public List<Client> findActive() {
        return search(null, true);
    }

    public Optional<Client> find(long id) {
        return DataAccess.read(connection -> clientDao.findById(connection, id));
    }

    public Client save(Client client) {
        validate(client);
        DataAccess.write(connection -> {
            if (client.getId() == null) {
                clientDao.insert(connection, client);
            } else {
                clientDao.update(connection, client);
            }
            return null;
        });
        return client;
    }

    public void setActive(long id, boolean active) {
        DataAccess.write(connection -> {
            clientDao.setActive(connection, id, active);
            return null;
        });
    }

    public void delete(long id) {
        DataAccess.write(connection -> {
            int documents = clientDao.countDocuments(connection, id);
            if (documents > 0) {
                throw new ValidationException("Ce client possède " + documents
                        + " document(s) : désactivez-le au lieu de le supprimer.");
            }
            clientDao.delete(connection, id);
            return null;
        });
    }

    /** Nettoie et valide les champs, lève une {@link ValidationException} si nécessaire. */
    public void validate(Client client) {
        List<String> errors = new ArrayList<>();
        client.setName(Texts.trimToNull(client.getName()));
        client.setContactName(Texts.trimToNull(client.getContactName()));
        client.setEmail(Texts.trimToNull(client.getEmail()));
        client.setPhone(Texts.trimToNull(client.getPhone()));
        client.setAddress(Texts.trimToNull(client.getAddress()));
        client.setCity(Texts.trimToNull(client.getCity()));
        client.setTaxId(Texts.trimToNull(client.getTaxId()));
        client.setNotes(Texts.trimToNull(client.getNotes()));

        if (client.getName() == null) {
            errors.add("La raison sociale du client est obligatoire.");
        } else if (client.getName().length() > 150) {
            errors.add("La raison sociale ne peut pas dépasser 150 caractères.");
        }
        if (client.getEmail() != null && !EMAIL.matcher(client.getEmail()).matches()) {
            errors.add("L'adresse e-mail du client n'est pas valide.");
        }
        if (client.getPhone() != null && client.getPhone().length() > 40) {
            errors.add("Le numéro de téléphone est trop long (40 caractères maximum).");
        }
        if (client.getTaxId() != null && client.getTaxId().length() > 40) {
            errors.add("L'identifiant fiscal est trop long (40 caractères maximum).");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
