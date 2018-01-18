package filipesluiz.jsf.faces;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UINamingContainer;
import javax.faces.component.UISelectOne;
import javax.faces.component.html.HtmlDataTable;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang.ObjectUtils;

/**
 * Nome    : GenericValidator
 * Objetivo: Executa validaï¿½ï¿½es genericas nos formulï¿½rios.
 * @since  : Data de criaï¿½ï¿½o 06/06/2013 18:04:26
 * @author   dmesquita
 * @version  $Revision: 1.0 $
 */
public abstract class GenericValidator implements Validator {
	
	protected abstract void initializeForm(UIComponent component) throws ParseException;
	
	protected String getValorTexto(UIComponent component, String campo){
		UIInput otherInput = (UIInput) component.findComponent(campo);
		return otherInput != null && otherInput.getSubmittedValue() != null ? (String) otherInput.getSubmittedValue().toString() : "";
	}
	
	protected String getValorCombo(UIComponent component, String campo){
		UISelectOne ui = (UISelectOne)component.findComponent(campo);
		return ui != null && ui.getSubmittedValue() != null ? ui.getSubmittedValue().toString() : "";
	}

	protected void setValorTexto(UIComponent component, String campo, Object valor){
		UIInput otherInput = (UIInput) component.findComponent(campo);
		
		if (otherInput != null){
			otherInput.setSubmittedValue(valor);
		}
	}
	
	protected String getValorBotao(UIComponent component, String campo){
		UICommand otherInput = (UICommand) component.findComponent(campo);
		return otherInput != null && otherInput.getValue() != null ? (String) otherInput.getValue() : "";
	}
	
	protected void resetFields(UIComponent uiComponent, String ...fields){
		
		if(fields == null){
			return;
		}
		
		for (String field : fields) {
			Object component = uiComponent.findComponent(field);
			
			if(component instanceof UIInput){
					((UIInput) component).resetValue();
			}
		}
	}
	
	//Atualiza tambem o valor na tela, mantendo o que submetido pelo usuario. Ticket 0007622.
	protected void reverterValor(UIComponent component, String id) {
		UIInput input = (UIInput) component.findComponent(id);
		input.setLocalValueSet(true);
		input.updateModel(FacesContext.getCurrentInstance());
	}
	
	private boolean equalsComponentValue(Object o1, Object o2) {
		return ObjectUtils.equals(o1, o2);
	}
	
	private boolean eAnscestralDe(UIComponent possivelDescendente, UIComponent ancestral) {
		UIComponent base = possivelDescendente;
		
		while(base != null) {
			if(ancestral.equals(base)) {
				return true;
			}
			
			base = base.getParent();
		}
		
		return false;
	}
	
	protected boolean isAlteradoCampo(UIComponent uiComponent){
		return isAlteradoCampo(uiComponent, FacesContext.getCurrentInstance().getViewRoot().getId());
	}
	
	/**.
	 * Mï¿½todo   : isAlteradoCampo
	 * Descriï¿½ï¿½o: 
	 * @param uiComponent
	 * @return
	 */
	protected boolean isAlteradoCampo(UIComponent uiComponent, String idViewLimitadora, String... componentesExcluidos){
		Map<String,String> mapParams = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
		Iterator<String> iKey = mapParams.keySet().iterator();
		FacesContext context = FacesContext.getCurrentInstance();
		Object asObject = null;
		Set<String> setComponentesExcluidos = new HashSet<String>(Arrays.asList(componentesExcluidos));
		UIComponent component = null, viewLimitadora = null;
		
		if(idViewLimitadora != null) {
			String separatorString = Character.toString(UINamingContainer.getSeparatorChar(FacesContext.getCurrentInstance()));
			String id = idViewLimitadora;
			
			if(! id.startsWith(separatorString)) {
				id = separatorString.concat(id);
			}
			
			viewLimitadora = uiComponent.findComponent(id);
		}
		
		while(iKey.hasNext()){
			String key = iKey.next();
					
			if(setComponentesExcluidos.contains(key)) {
				continue;
			}
			
			EditableValueHolder valueHolder = getComponent(uiComponent, key);
			
			if(valueHolder != null){

				if(valueHolder instanceof UIComponent) {
					component = (UIComponent) valueHolder;
				}
				
				if(idViewLimitadora != null && valueHolder instanceof UIComponent && ! eAnscestralDe(component, viewLimitadora)) {
					continue;
				}
				
				if(valueHolder.getConverter() == null) {
					if(! equalsComponentValue(valueHolder.getValue(), valueHolder.getSubmittedValue())) {
						return true;
					}
					
					continue;
				}
				
				try {
					asObject = valueHolder.getConverter().getAsObject(context, uiComponent, valueHolder.getSubmittedValue().toString());
				} catch (Exception e) {
					// Nao pode gerar mensagem de validacao
					// Pois a mesma ja ira ser feita na sua respectiva fase
				}
				
				if(! equalsComponentValue(valueHolder.getValue(), asObject)) {
					return true;
				}
			}
		}
		
		return false;
	}
	protected boolean isAlteradoCampoUnico(UIComponent uiComponent, String id){
		FacesContext context = FacesContext.getCurrentInstance();
		Object asObject = null;
		UIComponent component = null, viewLimitadora = null;
		
					
		viewLimitadora = uiComponent.findComponent(id);
		
		EditableValueHolder valueHolder = getComponent(uiComponent, id);
		
		if(valueHolder instanceof UIComponent) {
			component = (UIComponent) valueHolder;
		}
		
		if(valueHolder instanceof UIComponent && ! eAnscestralDe(component, viewLimitadora)) {
			return false;
		}
		
		if(valueHolder.getConverter() == null) {
			if(! equalsComponentValue(valueHolder.getValue(), valueHolder.getSubmittedValue())) {
				return true;
			}
			
			return false;
		}
		
		try {
			asObject = valueHolder.getConverter().getAsObject(context, uiComponent, valueHolder.getSubmittedValue().toString());
		} catch (Exception e) {
			// Nao pode gerar mensagem de validacao
			// Pois a mesma ja ira ser feita na sua respectiva fase
		}
		
		if(! equalsComponentValue(valueHolder.getValue(), asObject)) {
			return true;
		}
		
		return false;
	}
	
	private Map<String, List<Map<String, EditableValueHolder>>> rowsDataTable = new HashMap<String, List<Map<String ,EditableValueHolder>>>();
	
	private EditableValueHolder getComponent(UIComponent uiComponent, String id){
		String separatorChar = Character.toString(UINamingContainer.getSeparatorChar(FacesContext.getCurrentInstance()));
		String ids[] = id.split(separatorChar);
		
		if(ids.length == 1){
			UIComponent targetComponent = uiComponent.findComponent(id);
			
			if(targetComponent instanceof EditableValueHolder) {
				return (EditableValueHolder) targetComponent;
			}
			
			return null;
		}
		
		final HtmlDataTable table = (HtmlDataTable) uiComponent.findComponent(ids[0]);
		String componentId = join(ids, 2, separatorChar);
		
		return getComponentInTable(table, Integer.parseInt(ids[1]), componentId);
	}
	
	private EditableValueHolder getComponentInTable(HtmlDataTable table, int rowIndex, String componentId) {
		List<Map<String, EditableValueHolder>> list = null;
		
		if(table == null) {
			return null;
		}
		
		if(! rowsDataTable.containsKey(table.getId())){
			VisitCallbachTable visit = new VisitCallbachTable(table);
			table.visitTree(VisitContext.createVisitContext(FacesContext.getCurrentInstance()), visit);

			list = visit.getRowsDataTableInter();
	
			rowsDataTable.put(table.getId(),list );
		} else {
			list = rowsDataTable.get(table.getId());
		}
		
		return list.get(rowIndex).get(componentId);
	}
	
	private String join(String[] array, Integer fromIndex, String separator) {
		StringBuilder sb = new StringBuilder();
		int count = array.length;
		Integer auxIndex = fromIndex; 
		
		if(auxIndex == null) {
			auxIndex = 0;
		}
		
		sb.append(array[auxIndex++]);
		
		for(; auxIndex < count; ++auxIndex) {
			sb.append(separator).append(array[auxIndex]);
		}
		
		return sb.toString();
	}
	
	class VisitCallbachTable implements VisitCallback {

		public VisitCallbachTable(HtmlDataTable table){
			this.table = table;
			index = table.getRowIndex();
			firstColumn = table.getChildren().get(0);
			rowsDataTableInter.add(mapComponent);
		}
		
		private List<Map<String, EditableValueHolder>> rowsDataTableInter = new ArrayList<Map<String, EditableValueHolder>>();
		
		private HtmlDataTable table;
							
		private int index = 0;
		
		private Map<String, EditableValueHolder> mapComponent = new HashMap<String, EditableValueHolder>();
		
		private UIComponent firstColumn;
		
		private String separatorChar = Character.toString(UINamingContainer.getSeparatorChar(FacesContext.getCurrentInstance()));
		
			
				@Override
		public VisitResult visit(VisitContext context, UIComponent target) {
			/** A primeira linha ï¿½ o header da tabela. **/
			
			if(index == -1){
				index = table.getRowIndex();
				return VisitResult.ACCEPT;
			}
			
			if(index != table.getRowIndex()){
				mapComponent = new HashMap<String, EditableValueHolder>();
				rowsDataTableInter.add(mapComponent);
			}
			
			index = table.getRowIndex();
			
			if(target instanceof EditableValueHolder) {
				// Desconsidera a primeira coluna pq ï¿½ apenas de seleï¿½ï¿½o na tabela 
				if(target.getParent().getId().equals(firstColumn.getId())){
					return VisitResult.ACCEPT;
				}
				
				String targetClientId = target.getClientId(FacesContext.getCurrentInstance());
				
				if(targetClientId.contains(separatorChar)) {
					String[] clientIdSplitted = targetClientId.split(separatorChar);
					targetClientId = join(clientIdSplitted, 2, separatorChar);
				}
				
				mapComponent.put(targetClientId, CustomEditableValueHolder.build((EditableValueHolder) target));
			}
			
			return VisitResult.ACCEPT;
		}


				/**.
				 * @return Retorna a variavel: rowsDataTableInter
				 */
				public List<Map<String, EditableValueHolder>> getRowsDataTableInter() {
					return rowsDataTableInter;
				}


				/**.
				 * @param Atribui a variavel: rowsDataTableInter
				 */
				public void setRowsDataTableInter(
						List<Map<String, EditableValueHolder>> rowsDataTableInter) {
					this.rowsDataTableInter = rowsDataTableInter;
				}
				
				
	}
	
	/**
	 * Retorna true se o parametro idCommand foi o botao clicado.
	 * Metodo   : isActionEvent
	 * Descricao: 
	 * @param idCommand
	 * @return
	 */
	protected boolean isActionEvent(String idCommand){
		Map<String,String> mapParams = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
		
		// SE FOR UMA REQUISICAO AJAX ("javax.faces.partial.ajax" SETADO), DEVEMOS VERIFICAR
		// PELA FONTE DO EVENTO/ACAO PELO ATRIBUTO "javax.faces.source"
		if(mapParams.containsKey("javax.faces.partial.ajax")) {
			return mapParams.containsKey("javax.faces.source") && mapParams.get("javax.faces.source").equals(idCommand);
		}
		
		return mapParams.containsKey(idCommand);
	}
	
	/**
	 * .
	 * MÃ©todo   : validarNomeRecolhedor
	 * DescriÃ§Ã£o: 
	 * @return
	 */
	// Método comentado segundo RTC  1465124
	protected void validarNomeRecolhedor(final String valor, String campoMsg, UIInput input){
		
		// Chamado de melhoria 7286
	/*	if(contemErroNome(valor) 
				||!validarNome(valor)
				|| contem3letrasEmSequencia(valor)
				|| valor.contains("..")
				|| !isSomenteStringPonto(valor)
				|| !Pattern.matches("[a-zA-Z]{2,} .*[a-zA-Z]{2,}.*", valor)){
			// MANTIS 0011675
			enviarMsgErro("Nome " + campoMsg + " informado n\u00E3o \u00E9 v\u00E1lido.");
			//enviarMsgErro("Nome " + campoMsg + " informado n\u00E3o \u00E9 v\u00E1lido. O nome somente pode conter letras e pontos, deve conter pelo menos dois blocos com no m\u00EDnimo duas letras cada, n\u00E3o deve conter tr\u00EAs ou mais letras iguais consecutivas, n\u00E3o deve conter dois ou mais pontos consecutivos, n\u00E3o pode conter mais de tr\u00EAs abrevia\u00E7\u00F5es em sequ\u00EAncia.");
			
		}*/
		input.setSubmittedValue(valor.trim());
	}
	
	/**
	 * .
	 * MÃ©todo   : validarNomeRecolhedor
	 * DescriÃ§Ã£o: 
	 * @return
	 */
	protected void validarNomeTrabalhador(final String valor, String campoMsg, UIInput input){
		if(contemErroNome(valor) 
			|| !validarNome(valor)	
			|| contem3letrasEmSequencia(valor)
			|| valor.contains("..")
			|| !isSomenteStringPonto(valor)
			|| !Pattern.matches("[a-zA-Z\u00C0-\u00FF]{2,} .*[a-zA-Z\u00C0-\u00FF]{2,}.*", valor)){
			// MANTIS 0011675
			enviarMsgErro("Nome " + campoMsg + " informado n\u00E3o \u00E9 v\u00E1lido.");
			//enviarMsgErro("Nome " + campoMsg + " informado n\u00E3o \u00E9 v\u00E1lido. O nome somente pode conter letras e pontos, deve conter pelo menos dois blocos com no m\u00EDnimo duas letras cada, n\u00E3o deve conter tr\u00EAs ou mais letras iguais consecutivas, n\u00E3o deve conter dois ou mais pontos consecutivos, n\u00E3o pode conter mais de tr\u00EAs abrevia\u00E7\u00F5es em sequ\u00EAncia.");
	
		}
		
		input.setSubmittedValue(valor.trim());
	}
	
	private boolean contemErroNome(String valor){
		//Regra 1 - Nao deve existir mais de um espaco entre os nomes.
		if(valor.contains("  ")){
			return true;
		}
		
		CharSequence valorChar = valor;
		
		//Regra 3 - Nao pode ter mais de 3 letras isoladas (abreviacoes) em sequencia.
		if(Pattern.matches(".*[a-zA-Z\u00C0-\u00FF]{1}( |\\.|\\. )[a-zA-Z\u00C0-\u00FF]{1}( |\\.|\\. )[a-zA-Z\u00C0-\u00FF]{1}( |\\.|\\. )[a-zA-Z\u00C0-\u00FF]{1}( |\\.|\\.).*", valorChar)){
			return true;
		}
		
		return false;
	}
	
	private boolean contem3letrasEmSequencia(String valor){
		
		char[] valorChar = valor.replace(".", "").toCharArray();
		
		if(valorChar.length < 3){
			return false;
		}
		
		int count = 2;
		
		while(count < valorChar.length){
			if(valorChar[count] == valorChar[count-1] && valorChar[count-1] == valorChar[count-2]){
				return true;
			}
			count++;
		}
		
		return false;
	}
	
	
	public boolean validarNome(String nome) {
		if(nome == null || nome.isEmpty()) {
			return false;
		}
		
		if(nome.charAt(0) == ' ') {
			return false;
		}
		
		if(nome.contains("  ")) {
			return false;
		}
		
		char ultimaLetra = 0;
		int contador = 0;
		
		for(int i = 0; i < nome.length(); ++i) {
			char letra = nome.charAt(i);
			
			if(letra == ultimaLetra) {
				contador++;
			}
			
			if(contador >= 2) {
				return false;
			}
			
			ultimaLetra = letra;
		}
		
		return true;
	}
	
	/**
	 * Criado pois o Regex estava dando erro para numero no meio da String ex."teste 2 teste"
	 * MÃ©todo   : isSomenteStringPonto
	 * DescriÃ§Ã£o: 
	 * @param value
	 * @return
	 */
	public boolean isSomenteStringPonto(String value) {
		
		char[] chars = value.replace(".", "").replace(" ", "").toCharArray();
		
	    for (char c : chars) {
	        if(!Character.isLetter(c)) {
	            return false;
	        }
	    }

	    return true;
	}
	
	private void enviarMsgErro(String msg){
		//throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_INFO, GenericManagedBean.getMensagemNote(NoteMessage.MA001, "Nome") , "note-message"));
		throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_INFO, msg, "mensagem"));
	}
	
	public void enviarMsg(String key, Object... params){
		String mensagem = GenericManagedBean.getMensagemNote(key, params);
		enviarMsgErro(mensagem);
	}
	
	
	
}
